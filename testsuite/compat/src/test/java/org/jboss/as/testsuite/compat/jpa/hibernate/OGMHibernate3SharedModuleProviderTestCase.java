/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.testsuite.compat.jpa.hibernate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
// @Ignore  // until we hack the test to populate the org.hibernate:ogm module
public class OGMHibernate3SharedModuleProviderTestCase {

    private static final String ARCHIVE_NAME = "hibernate3module_test";

    private static final String infinispan_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <!--\n" +
            "    ~ Hibernate, Relational Persistence for Idiomatic Java\n" +
            "    ~\n" +
            "    ~ JBoss, Home of Professional Open Source\n" +
            "    ~ Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors\n" +
            "    ~ as indicated by the @authors tag. All rights reserved.\n" +
            "    ~ See the copyright.txt in the distribution for a\n" +
            "    ~ full listing of individual contributors.\n" +
            "    ~\n" +
            "    ~ This copyrighted material is made available to anyone wishing to use,\n" +
            "    ~ modify, copy, or redistribute it subject to the terms and conditions\n" +
            "    ~ of the GNU Lesser General Public License, v. 2.1.\n" +
            "    ~ This program is distributed in the hope that it will be useful, but WITHOUT A\n" +
            "    ~ WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A\n" +
            "    ~ PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.\n" +
            "    ~ You should have received a copy of the GNU Lesser General Public License,\n" +
            "    ~ v.2.1 along with this distribution; if not, write to the Free Software\n" +
            "    ~ Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,\n" +
            "    ~ MA 02110-1301, USA.\n" +
            "    -->\n" +
            "    <!--\n" +
            "    This is the testing configuration, running in LOCAL clustering mode to speedup tests.\n" +
            "    Not all tests use this, so that at least one still validates the configuration\n" +
            "    validity which would be shipped with a release (like JPAStandaloneTest).\n" +
            "    -->\n" +
            "    <infinispan\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "    xsi:schemaLocation=\"urn:infinispan:config:5.0 http://www.infinispan.org/schemas/infinispan-config-5.0.xsd\"\n" +
            "    xmlns=\"urn:infinispan:config:5.0\">\n" +
            "     \n" +
            "    <global>\n" +
            "    </global>\n" +
            "     \n" +
            "    <!-- *************************** -->\n" +
            "    <!-- Default cache settings -->\n" +
            "    <!-- *************************** -->\n" +
            "     \n" +
            "    <default>\n" +
            "    </default>\n" +
            "     \n" +
            "    <!-- *************************************** -->\n" +
            "    <!-- Cache to store the OGM entities -->\n" +
            "    <!-- *************************************** -->\n" +
            "    <namedCache\n" +
            "    name=\"ENTITIES\">\n" +
            "    </namedCache>\n" +
            "     \n" +
            "    <!-- *********************************************** -->\n" +
            "    <!-- Cache to store the relations across entities -->\n" +
            "    <!-- *********************************************** -->\n" +
            "    <namedCache\n" +
            "    name=\"ASSOCIATIONS\">\n" +
            "    </namedCache>\n" +
            "     \n" +
            "    <!-- ***************************** -->\n" +
            "    <!-- Cache to store identifiers -->\n" +
            "    <!-- ***************************** -->\n" +
            "    <namedCache\n" +
            "    name=\"IDENTIFIERS\">\n" +
            "    </namedCache>\n" +
            "     \n" +
            "    </infinispan>\n"
        ;


    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"hibernate3_pc\">" +
            "<provider>org.hibernate.ogm.jpa.HibernateOgmPersistence</provider>" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"none\"/>" +
            "<property name=\"hibernate.ogm.infinispan.configuration_resourcename\" value=\"infinispan.xml\"/>"+
            // "<property name=\"hibernate.dialect\" value=\"org.hibernate.ogm.dialect.NoopDialect\"/>" +
            // "<property name=\"hibernate.show_sql\" value=\"true\"/>" +
            "<property name=\"jboss.as.jpa.providerModule\" value=\"org.hibernate:ogm\"/>" +
            "<property name=\"jboss.as.jpa.adapterModule\" value=\"org.jboss.as.jpa.hibernate:3\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SFSB1.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        lib.addAsResource(new StringAsset(infinispan_xml), "infinispan.xml");

        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(OGMHibernate3SharedModuleProviderTestCase.class);
        ear.addAsModule(main);

        return ear;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!" + interfaceType.getName()));
        } catch (NamingException e) {
            dumpJndi("");
            throw e;
        }
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private static void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private static void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(ncp.getName());
            } else {
                dumpJndi(s + "/" + ncp.getName());
            }
        }
    }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 10);
        sfsb1.createEmployee("Alex Scott", "London, England", 20);
        sfsb1.getEmployeeNoTX(10);
        sfsb1.getEmployeeNoTX(20);
    }

}
