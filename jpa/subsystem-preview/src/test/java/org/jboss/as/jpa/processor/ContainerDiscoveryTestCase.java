/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.processor;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.spi.Discoverable;
import org.jboss.as.jpa.config.PersistenceUnitMetadataImpl;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContainerDiscoveryTestCase {
    @Entity
    public static class RootEntity { }
    @Entity
    public static class ListedEntity { }
    @Entity
    public static class UnrelatedEntity { }
    @Discoverable
    @Repeatable(Customs.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Custom { }
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Customs { Custom[] value(); }
    @Custom @Custom
    public static class CustomType { }

    @Test
    public void archivesExclusionXmlAndCustomDiscovery() throws Exception {
        Path root = Files.createTempDirectory("pu-root");
        Path listed = Files.createTempDirectory("pu-listed");
        try {
            copyClass(root, RootEntity.class);
            copyClass(root, CustomType.class);
            copyClass(listed, ListedEntity.class);
            Files.createDirectories(root.resolve("META-INF"));
            Files.writeString(root.resolve("META-INF/orm.xml"),
                    "<entity-mappings><package>xml.only</package><entity class=\"Mapped\"/></entity-mappings>");
            PersistenceUnitMetadataImpl metadata = new PersistenceUnitMetadataImpl();
            metadata.setClassLoader(getClass().getClassLoader());
            metadata.setPersistenceUnitRootUrl(root.toUri().toURL());
            Path jar = listed.resolve("listed.jar");
            String listedResource = ListedEntity.class.getName().replace('.', '/') + ".class";
            try (java.util.jar.JarOutputStream output = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
                output.putNextEntry(new java.util.jar.JarEntry(listedResource));
                Files.copy(listed.resolve(listedResource), output);
                output.closeEntry();
            }
            metadata.setJarFileUrls(List.of(jar.toUri().toURL()));
            metadata.setManagedClassNames(List.of("explicit.Type"));
            DeploymentUnit deployment = (DeploymentUnit) Proxy.newProxyInstance(DeploymentUnit.class.getClassLoader(),
                    new Class<?>[]{DeploymentUnit.class}, (proxy, method, args) ->
                            method.getName().equals("getAttachmentList") ? List.of() : null);
            ContainerManagedEntityScanner.addManagedClassNames(metadata, deployment);
            assertTrue(metadata.getAllClassNames().contains(RootEntity.class.getName()));
            assertTrue(metadata.getAllClassNames().contains(ListedEntity.class.getName()));
            assertTrue(metadata.getAllClassNames().contains(CustomType.class.getName()));
            assertTrue(metadata.getAllClassNames().contains("xml.only.Mapped"));
            assertFalse(metadata.getAllClassNames().contains(UnrelatedEntity.class.getName()));
            assertEquals(List.of("explicit.Type"), metadata.getManagedClassNames());
            metadata.setExcludeUnlistedClasses(true);
            ContainerManagedEntityScanner.addManagedClassNames(metadata, deployment);
            assertFalse(metadata.getAllClassNames().contains(RootEntity.class.getName()));
            assertTrue(metadata.getAllClassNames().contains(ListedEntity.class.getName()));
            assertTrue(metadata.getAllClassNames().contains("xml.only.Mapped"));
        } finally {
            deleteTree(root);
            deleteTree(listed);
        }
    }

    private static void copyClass(Path root, Class<?> type) throws Exception {
        String resource = type.getName().replace('.', '/') + ".class";
        Path target = root.resolve(resource);
        Files.createDirectories(target.getParent());
        try (java.io.InputStream stream = type.getClassLoader().getResourceAsStream(resource)) {
            Files.copy(stream, target);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new)) {
                Files.delete(path);
            }
        }
    }
}
