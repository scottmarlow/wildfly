/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Discovers a persistence unit's managed classes (entities, embeddables, mapped superclasses and attribute
 * converters) from the deployment's Jandex index and adds them to the persistence unit metadata.
 * <p>
 * This is required for persistence providers that no longer scan the persistence unit archive during
 * container-managed bootstrap (Jakarta Persistence 4 / Hibernate ORM 8), which instead rely on
 * {@link jakarta.persistence.spi.PersistenceUnitInfo#getManagedClassNames()}. It is only invoked when the
 * provider adaptor reports {@code containerManagedEntityScanning() == true}; providers that scan the archive
 * themselves (Hibernate ORM 7, EclipseLink) are unaffected.
 * <p>
 * Scanning is scoped to the archives that make up the persistence unit &mdash; the persistence unit root plus
 * any {@code <jar-file>} references &mdash; by matching their URLs to the deployment's resource roots and using
 * each root's annotation index. If those archives cannot be resolved to indexed resource roots, the whole
 * deployment's composite annotation index is used as a fallback.
 *
 * @author Scott Marlow
 */
final class ContainerManagedEntityScanner {

    private static final DotName[] MANAGED_CLASS_ANNOTATIONS = {
            DotName.createSimple("jakarta.persistence.Entity"),
            DotName.createSimple("jakarta.persistence.Embeddable"),
            DotName.createSimple("jakarta.persistence.MappedSuperclass"),
            DotName.createSimple("jakarta.persistence.Converter"),
    };

    private ContainerManagedEntityScanner() {
    }

    /**
     * Adds the managed class names discovered in the persistence unit's archives to the persistence unit, unless
     * it opted out of scanning via {@code <exclude-unlisted-classes>true</exclude-unlisted-classes>}. Any classes
     * already listed (for example via {@code <class>} elements) are preserved.
     *
     * @param pu             the persistence unit metadata to augment
     * @param deploymentUnit the deployment unit the persistence unit belongs to
     */
    static void addManagedClassNames(PersistenceUnitMetadata pu, DeploymentUnit deploymentUnit) {
        if (pu.excludeUnlistedClasses()) {
            return;
        }

        // preserve any explicitly listed classes and keep a stable, de-duplicated ordering
        final Set<String> managedClassNames = new LinkedHashSet<>(pu.getManagedClassNames());

        // the archives that make up the persistence unit: its root plus any <jar-file> references
        final Set<VirtualFile> persistenceUnitArchives = persistenceUnitArchives(pu);

        boolean scannedResourceRoot = false;
        if (!persistenceUnitArchives.isEmpty()) {
            for (ResourceRoot resourceRoot : resourceRoots(deploymentUnit)) {
                if (persistenceUnitArchives.contains(resourceRoot.getRoot())) {
                    final Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                    if (index != null) {
                        for (DotName annotation : MANAGED_CLASS_ANNOTATIONS) {
                            addClassTargets(index.getAnnotations(annotation), managedClassNames);
                        }
                        scannedResourceRoot = true;
                    }
                }
            }
        }

        // Fallback for the common single-archive cases where the persistence unit archives could not be matched
        // to indexed resource roots: scan the whole deployment's composite annotation index.
        if (!scannedResourceRoot) {
            final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            if (compositeIndex != null) {
                for (DotName annotation : MANAGED_CLASS_ANNOTATIONS) {
                    addClassTargets(compositeIndex.getAnnotations(annotation), managedClassNames);
                }
            }
        }

        pu.setManagedClassNames(new ArrayList<>(managedClassNames));
        JpaLogger.ROOT_LOGGER.tracef("container discovered %d managed class(es) for persistence unit '%s'",
                managedClassNames.size(), pu.getScopedPersistenceUnitName());
    }

    /**
     * Resolves the persistence unit root URL and any {@code <jar-file>} reference URLs to their virtual files.
     */
    private static Set<VirtualFile> persistenceUnitArchives(PersistenceUnitMetadata pu) {
        final Set<VirtualFile> archives = new LinkedHashSet<>();
        addArchive(archives, pu.getPersistenceUnitRootUrl());
        if (pu.getJarFileUrls() != null) {
            for (URL jarFileUrl : pu.getJarFileUrls()) {
                addArchive(archives, jarFileUrl);
            }
        }
        return archives;
    }

    private static void addArchive(Set<VirtualFile> archives, URL url) {
        if (url == null) {
            return;
        }
        try {
            archives.add(VFS.getChild(url.toURI()));
        } catch (URISyntaxException e) {
            // ignore an archive we cannot resolve; the composite-index fallback still applies
            JpaLogger.ROOT_LOGGER.tracef("unable to resolve persistence unit archive URL %s for scanning", url);
        }
    }

    /**
     * Collects the resource roots of the deployment unit and, for sub-deployments, its parent (so that
     * enterprise-archive libraries referenced via {@code <jar-file>} are included).
     */
    private static List<ResourceRoot> resourceRoots(DeploymentUnit deploymentUnit) {
        final List<ResourceRoot> resourceRoots = new ArrayList<>(DeploymentUtils.allResourceRoots(deploymentUnit));
        final DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            resourceRoots.addAll(DeploymentUtils.allResourceRoots(parent));
        }
        return resourceRoots;
    }

    private static void addClassTargets(Collection<AnnotationInstance> annotations, Set<String> managedClassNames) {
        if (annotations == null) {
            return;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                managedClassNames.add(annotationInstance.target().asClass().name().toString());
            }
        }
    }
}
