/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate "hibernate.ejb.use_class_enhancer" test that causes hibernate to add a
 * javax.persistence.spi.ClassTransformer to the pu.
 *
 * TODO: rename to org.jboss.as.test.integration.jpa.mockprovider.MockProviderTestCase so this can include other unit tests.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ClassFileTransformerTestCase {

    private static final String ARCHIVE_NAME = "jpa_classTransformerTestWithMockProvider";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive persistenceProvider = ShrinkWrap.create(JavaArchive.class, "testpersistenceprovider.jar");
        persistenceProvider.addClasses(
                    TestClassTransformer.class,
                    TestEntityManagerFactory.class,
                    TestPersistenceProvider.class
                );

        // META-INF/services/javax.persistence.spi.PersistenceProvider
        persistenceProvider.addAsResource(new StringAsset("org.jboss.as.test.integration.jpa.mockprovider.classtransformer.TestPersistenceProvider"),
                "META-INF/services/javax.persistence.spi.PersistenceProvider");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, "ejbjar.jar");
        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addClasses(ClassFileTransformerTestCase.class,
                SFSB1.class
        );
        ejbjar.addAsManifestResource(ClassFileTransformerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class, ClassFileTransformerTestCase.class);
        ear.addAsLibraries(lib, persistenceProvider);
        return ear;

    }

    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "   \n" +
                "</ejb-jar>");
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void test_use_class_enhancer() throws Exception {
        try {
            assertTrue("entity classes are enhanced", TestClassTransformer.getTransformedClasses().size() > 0 );
        } finally {
            TestClassTransformer.clearTransformedClasses();
        }
    }

    @Test
    public void test_persistenceUnitInfoURLS() throws Exception {
        try {
            assertTrue("testing that PersistenceUnitInfo.getPersistenceUnitRootUrl() url is file based, failed because getPersistenceUnitRootUrl is " +
                    TestPersistenceProvider.getLastPersistenceUnitInfo().getPersistenceUnitRootUrl().getProtocol(),
                    "file".equals(TestPersistenceProvider.getLastPersistenceUnitInfo().getPersistenceUnitRootUrl().getProtocol()));
            URL rootUrl = TestPersistenceProvider.getLastPersistenceUnitInfo().getPersistenceUnitRootUrl();
            URLConnection urlConnection = rootUrl.openConnection();
            // should be ejbjar.jar
            assertTrue("getPersistenceUnitRootUrl() returns a JarURLConnection instance.  actually got=" + urlConnectionDetails(urlConnection), urlConnection instanceof JarURLConnection);


        } finally {
            TestPersistenceProvider.clearLastPersistenceUnitInfo();
        }
    }

    private String urlConnectionDetails(URLConnection urlConnection) {
        String result = null;
        try {
            result = "URLConnection is an instance of " +
                    urlConnection.getClass().getName()  +
                    ".  getContent() = " + urlConnection.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            result += "stream contexts=";
            result += reader.readLine();
            String line;
            while((line=reader.readLine())!=null){
                result+=line;
            }
        } catch (IOException e) {
            return "couldn't get content, caught error " + e.getMessage();
        }
        return result;

    }

}
