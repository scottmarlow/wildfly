/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
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
     * Adds the managed class names discovered in the deployment index to the persistence unit, unless the
     * persistence unit opted out of scanning via {@code <exclude-unlisted-classes>true</exclude-unlisted-classes>}.
     * Any classes already listed (for example via {@code <class>} elements) are preserved.
     *
     * @param pu             the persistence unit metadata to augment
     * @param deploymentUnit the deployment unit whose annotation index is scanned
     */
    static void addManagedClassNames(PersistenceUnitMetadata pu, DeploymentUnit deploymentUnit) {
        if (pu.excludeUnlistedClasses()) {
            return;
        }
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        // preserve any explicitly listed classes and keep a stable, de-duplicated ordering
        final Set<String> managedClassNames = new LinkedHashSet<>(pu.getManagedClassNames());
        for (DotName annotation : MANAGED_CLASS_ANNOTATIONS) {
            final List<AnnotationInstance> annotations = index.getAnnotations(annotation);
            if (annotations == null) {
                continue;
            }
            for (AnnotationInstance annotationInstance : annotations) {
                if (annotationInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    managedClassNames.add(annotationInstance.target().asClass().name().toString());
                }
            }
        }

        pu.setManagedClassNames(new ArrayList<>(managedClassNames));
        JpaLogger.ROOT_LOGGER.tracef("container discovered %d managed class(es) for persistence unit '%s'",
                managedClassNames.size(), pu.getScopedPersistenceUnitName());
    }
}
