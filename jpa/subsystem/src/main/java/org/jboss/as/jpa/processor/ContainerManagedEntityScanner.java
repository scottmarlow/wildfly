/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Indexer;
import org.jboss.as.server.deployment.module.ResourceRoot;
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
 * {@code PersistenceUnitInfo.getAllClassNames()}. It is only invoked when the
 * provider adaptor reports {@code containerManagedEntityScanning() == true}; providers that scan the archive
 * themselves (Hibernate ORM 7, EclipseLink) are unaffected.
 * <p>
 * Scanning is scoped to the archives that make up the persistence unit &mdash; the persistence unit root plus
 * any {@code <jar-file>} references &mdash; by matching their URLs to the deployment's resource roots and using
 * each root's annotation index. Unindexed archives are indexed locally, without scanning unrelated deployments.
 * Explicit mapping files are also resolved through the persistence unit classloader.
 *
 * @author Scott Marlow
 */
final class ContainerManagedEntityScanner {

    private ContainerManagedEntityScanner() {
    }

    /**
     * Adds the managed class names discovered in the persistence unit's archives to the complete inventory.
     * Exclusion suppresses root discovery while listed JARs are still scanned. Any classes
     * already listed (for example via {@code <class>} elements) are preserved.
     *
     * @param pu             the persistence unit metadata to augment
     * @param deploymentUnit the deployment unit the persistence unit belongs to
     */
    static void addManagedClassNames(PersistenceUnitMetadata pu, DeploymentUnit deploymentUnit) {
        final Set<String> classes = new LinkedHashSet<>(pu.getManagedClassNames());
        final Set<String> packages = new LinkedHashSet<>(pu.getManagedPackageDescriptors());
        final Set<String> modules = new LinkedHashSet<>(pu.getManagedModuleDescriptors());
        final Map<DotName, Boolean> discoverable = new HashMap<>();
        for (VirtualFile archive : persistenceUnitArchives(pu)) {
            Index index = null;
            for (ResourceRoot root : resourceRoots(deploymentUnit)) {
                if (archive.equals(root.getRoot())) {
                    index = root.getAttachment(Attachments.ANNOTATION_INDEX);
                    break;
                }
            }
            try {
                if (index == null) {
                    Indexer indexer = new Indexer();
                    if (archive.isFile()) {
                        try (java.util.jar.JarInputStream stream = new java.util.jar.JarInputStream(archive.openStream())) {
                            java.util.jar.JarEntry entry;
                            while ((entry = stream.getNextJarEntry()) != null) {
                                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                    indexer.index(stream);
                                }
                            }
                        }
                    } else {
                        for (VirtualFile file : archive.getChildrenRecursively(file -> file.isFile() && file.getName().endsWith(".class"))) {
                            try (InputStream stream = file.openStream()) {
                                indexer.index(stream);
                            }
                        }
                    }
                    index = indexer.complete();
                }
                for (org.jboss.jandex.ModuleInfo module : index.getKnownModules()) {
                    if (module.annotations().stream().anyMatch(annotation ->
                            isDiscoverable(annotation.name(), pu.getClassLoader(), discoverable))) {
                        modules.add(module.name().toString());
                    }
                }
                for (ClassInfo type : index.getKnownClasses()) {
                    if (type.declaredAnnotations().stream().anyMatch(annotation ->
                            isDiscoverable(annotation.name(), pu.getClassLoader(), discoverable))) {
                        String name = type.name().toString();
                        if (type.module() != null) {
                            modules.add(type.module().name().toString());
                        } else if (name.endsWith(".package-info")) {
                            packages.add(name.substring(0, name.length() - ".package-info".length()));
                        } else if (!name.equals("module-info")) {
                            classes.add(name);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot index persistence unit archive " + archive, e);
            }
        }
        // XML mappings apply even when annotation discovery is excluded.
        Set<String> mappings = new LinkedHashSet<>(pu.getMappingFileNames());
        // Listed resources may live in classpath-visible libraries outside the unit's archives.
        // Only explicit mappings use the classloader; implicit orm.xml discovery stays archive-scoped.
        for (String mapping : mappings) {
            try (InputStream stream = pu.getClassLoader().getResourceAsStream(mapping)) {
                if (stream != null) {
                    addXmlClasses(stream, classes);
                }
            } catch (IOException | XMLStreamException e) {
                throw new IllegalStateException("Cannot read persistence mapping " + mapping + " from the persistence unit classloader", e);
            }
        }
        mappings.add("META-INF/orm.xml");
        for (String mapping : mappings) {
            Set<VirtualFile> mappingArchives = persistenceUnitArchives(pu);
            addArchive(mappingArchives, pu.getPersistenceUnitRootUrl());
            for (VirtualFile archive : mappingArchives) {
                try (InputStream stream = mappingStream(archive, mapping)) {
                    if (stream != null) {
                        addXmlClasses(stream, classes);
                    }
                } catch (IOException | XMLStreamException e) {
                    throw new IllegalStateException("Cannot read persistence mapping " + mapping + " in " + archive, e);
                }
            }
        }
        pu.setAllClassNames(new ArrayList<>(classes));
        pu.setAllPackageDescriptors(new ArrayList<>(packages));
        pu.setAllModuleDescriptors(new ArrayList<>(modules));
        JpaLogger.ROOT_LOGGER.tracef("container discovered %d managed class(es) for persistence unit '%s'",
                classes.size(), pu.getScopedPersistenceUnitName());
    }

    private static InputStream mappingStream(VirtualFile archive, String mapping) throws IOException {
        if (archive.isFile()) {
            try (java.util.jar.JarInputStream stream = new java.util.jar.JarInputStream(archive.openStream())) {
                java.util.jar.JarEntry entry;
                while ((entry = stream.getNextJarEntry()) != null) {
                    if (entry.getName().equals(mapping)) {
                        return new java.io.ByteArrayInputStream(stream.readAllBytes());
                    }
                }
            }
            return null;
        }
        VirtualFile file = archive.getChild(mapping);
        return file.exists() ? file.openStream() : null;
    }

    private static boolean isDiscoverable(DotName name, ClassLoader loader, Map<DotName, Boolean> cache) {
        Boolean result = cache.get(name);
        if (result != null) {
            return result;
        }
        // Seed before inspecting repeatable containers to break recursive definitions.
        cache.put(name, false);
        boolean discovered = name.toString().equals("jakarta.data.repository.Repository");
        try (InputStream stream = loader.getResourceAsStream(name.toString().replace('.', '/') + ".class")) {
            if (stream != null) {
                Indexer indexer = new Indexer();
                indexer.index(stream);
                ClassInfo annotation = indexer.complete().getClassByName(name);
                discovered |= annotation.hasDeclaredAnnotation(DotName.createSimple("jakarta.persistence.spi.Discoverable"));
                // A repeatable container's value() returns the discoverable annotation array.
                for (org.jboss.jandex.MethodInfo method : annotation.methods()) {
                    if (method.name().equals("value") && method.returnType().kind() == org.jboss.jandex.Type.Kind.ARRAY) {
                        DotName component = method.returnType().asArrayType().component().name();
                        try (InputStream componentStream = loader.getResourceAsStream(component.toString().replace('.', '/') + ".class")) {
                            if (componentStream != null) {
                                Indexer componentIndexer = new Indexer();
                                componentIndexer.index(componentStream);
                                ClassInfo componentType = componentIndexer.complete().getClassByName(component);
                                org.jboss.jandex.AnnotationInstance repeatable = componentType.declaredAnnotation(
                                        DotName.createSimple("java.lang.annotation.Repeatable"));
                                if (repeatable != null && repeatable.value().asClass().name().equals(name)) {
                                    discovered |= isDiscoverable(component, loader, cache);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read annotation " + name, e);
        }
        cache.put(name, discovered);
        return discovered;
    }

    private static void addXmlClasses(InputStream stream, Set<String> classes) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        XMLStreamReader reader = factory.createXMLStreamReader(stream);
        String packageName = "";
        List<String> names = new ArrayList<>();
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    String element = reader.getLocalName();
                    if (element.equals("package")) {
                        packageName = reader.getElementText().trim();
                    } else if (element.equals("entity") || element.equals("embeddable")
                            || element.equals("mapped-superclass") || element.equals("converter")) {
                        String name = reader.getAttributeValue(null, "class");
                        if (name != null) {
                            names.add(name);
                        }
                    }
                }
            }
            for (String name : names) {
                classes.add(name.contains(".") || packageName.isEmpty() ? name : packageName + "." + name);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Resolves the persistence unit root URL and any {@code <jar-file>} reference URLs to their virtual files.
     */
    private static Set<VirtualFile> persistenceUnitArchives(PersistenceUnitMetadata pu) {
        final Set<VirtualFile> archives = new LinkedHashSet<>();
        if (!pu.excludeUnlistedClasses()) {
            addArchive(archives, pu.getPersistenceUnitRootUrl());
        }
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
            throw new IllegalArgumentException("Cannot resolve persistence unit archive URL " + url, e);
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

}
