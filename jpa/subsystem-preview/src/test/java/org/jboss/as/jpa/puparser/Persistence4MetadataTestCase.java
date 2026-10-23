/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.puparser;

import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import jakarta.persistence.FetchType;
import org.jboss.as.jpa.config.PersistenceUnitMetadataImpl;
import org.jboss.metadata.property.PropertyReplacers;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class Persistence4MetadataTestCase {
    @Test
    public void descriptorsAndCompleteInventories() throws Exception {
        String xml = "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"4.0\">"
                + "<persistence-unit name=\"test\"><class>example.Explicit</class>"
                + "<package-descriptor>example</package-descriptor>"
                + "<module-descriptor>example.module</module-descriptor>"
                + "<default-to-one-fetch-type>LAZY</default-to-one-fetch-type>"
                + "</persistence-unit></persistence>";
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml));
        try {
            PersistenceUnitMetadataImpl metadata = (PersistenceUnitMetadataImpl) PersistenceUnitXmlParser
                    .parse(reader, PropertyReplacers.noop()).getPersistenceUnits().get(0);
            assertEquals("4.0", metadata.getPersistenceXMLSchemaVersion());
            assertEquals(FetchType.LAZY, metadata.getDefaultToOneFetchType());
            assertEquals(List.of("example"), metadata.getManagedPackageDescriptors());
            assertEquals(List.of("example.module"), metadata.getManagedModuleDescriptors());
            metadata.setAllClassNames(List.of("example.Explicit", "example.Discovered"));
            metadata.setAllPackageDescriptors(List.of("example", "discovered"));
            metadata.setAllModuleDescriptors(List.of("example.module", "discovered.module"));
            assertEquals(List.of("example.Explicit"), metadata.getManagedClassNames());
            assertEquals(2, metadata.getAllClassNames().size());
            assertEquals(1, metadata.getManagedPackageDescriptors().size());
            assertEquals(2, metadata.getAllPackageDescriptors().size());
            assertEquals(1, metadata.getManagedModuleDescriptors().size());
            assertEquals(2, metadata.getAllModuleDescriptors().size());
        } finally {
            reader.close();
        }
    }

    @Test
    public void defaultFetchType() {
        assertEquals(FetchType.EAGER, new PersistenceUnitMetadataImpl().getDefaultToOneFetchType());
    }
}
