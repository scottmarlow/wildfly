/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hhh18901;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jpa.jarfile.JarFileEntity;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Reproducer for https://hibernate.atlassian.net/browse/HHH-18901 based on TCK test classes currently.
 * TODO: after reproducing eliminate the used TCK Entity classes by replacing with bare minimum needed classes.
 */
@RunWith(Arquillian.class)
public class JpaJarFileTestCase {

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "hhh18901.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(JarFileEntity.class);
        jar.addClass(Employee.class);
        jar.addClass(Project.class);
        jar.addClass(PartTimeEmployee.class);
        jar.addClass(Department.class);
        jar.addClass(AbstractPersonnel.class);
        jar.addClass(MainArchiveEntity.class);
        // jar.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(jar);

        // With change to only loop twice through this code, the failure doesn't occur on first run but always on second run.
        // For more consistent failures we can probably loop a bit more.
        for (int looper = 1 ; looper < 3; looper++ ) {
            JavaArchive clientModule = ShrinkWrap.create(JavaArchive.class,looper + "-notappclientcontainer.jar");
            clientModule.addClasses(JpaJarFileTestCase.class, JpaTestSlsb.class);
            clientModule.addAsManifestResource( JpaJarFileTestCase.class.getPackage(), "application-client.xml","META-INF/application-client.xml");
            clientModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(clientModule);
        }
        return ear;
    }


    @Test
    public void testEntityInMainArchive() throws NamingException {
        PartTimeEmployee[] ptRef = new PartTimeEmployee[5];

    }

    @Test
    public void testEntityInJarFileArchive() throws NamingException {

    }

}
