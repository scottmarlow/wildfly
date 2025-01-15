/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.appclient;

import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * https://issues.redhat.com/browse/WFLY-20277
 * Verify that persistence units in app-client container archive are ignored when deploying on server
 */
@RunWith(Arquillian.class)
public class PersistenceUnitInAppClientArchiveInServerTestCase {

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "hhh18901.ear");

        JavaArchive clientModule = ShrinkWrap.create(JavaArchive.class,"appclientcontainerarchive.jar");
        clientModule.addClasses(PersistenceUnitInAppClientArchiveInServerTestCase.class);
        clientModule.addAsManifestResource( PersistenceUnitInAppClientArchiveInServerTestCase.class.getPackage(), "application-client.xml","application-client.xml");
        clientModule.addAsManifestResource(PersistenceUnitInAppClientArchiveInServerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(clientModule);
        return ear;
    }

    @PersistenceUnit(unitName = "mainPu")
    EntityManagerFactory entityManagerFactory;

    @Test
    public void testEntityInJarFileArchive() throws NamingException {
        assertTrue("entityManagerFactory should be null in server as it is part of an application-client archive.", entityManagerFactory == null );
    }

}
