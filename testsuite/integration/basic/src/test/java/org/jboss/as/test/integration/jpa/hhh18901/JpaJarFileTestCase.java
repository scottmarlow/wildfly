/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hhh18901;


import javax.naming.NamingException;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;



/**
 * Reproducer for https://hibernate.atlassian.net/browse/HHH-18901 based on TCK test classes currently.
 * TODO: after reproducing eliminate the used TCK Entity classes by replacing with bare minimum needed classes before creating pull request.
 */
@RunWith(Arquillian.class)
public class JpaJarFileTestCase {

    @Deployment
    public static Archive<?> deploy() {

        /**
         * jpa_core_annotations_access_property.jar has:
         * META-INF/persistence.xml
         * com/sun/ts/tests/jpa/core/types/common/Grade.class
         * com/sun/ts/tests/jpa/core/annotations/access/property/DataTypes.class
         * com/sun/ts/tests/jpa/core/annotations/access/property/DataTypes2.class
         *
         * jpa_core_annotations_access_property_pmservlet_vehicle_web.war
         * jpa_core_annotations_access_property_puservlet_vehicle_web.war
         * jpa_core_annotations_access_property_vehicles.ear
         */
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "hhh18901.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addClasses(CdiTestBean.class, DataTypes.class, DataTypes2.class, Grade.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        ear.addAsLibrary(jar);

        for (int looper = 1; looper < 10; looper++) {
            JavaArchive clientModule = ShrinkWrap.create(JavaArchive.class, looper + "-notappclientcontainer.jar");
            clientModule.addClasses(JpaJarFileTestCase.class, DataTypes.class, DataTypes.class, Grade.class);

            clientModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "application-client.xml", "application-client.xml");
            clientModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(clientModule);
        }

        for (int looper = 1; looper < 10; looper++) {
            JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class, looper + "-ejb-module.jar");
            ejbModule.addClasses(JpaJarFileTestCase.class, DataTypes.class, DataTypes.class, Grade.class);

            ejbModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(ejbModule);
        }

        for (int looper = 1; looper < 10; looper++) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, looper + "-NonTransactionalEmTestCase.war");
            war.addClasses(CdiTestBean.class, JpaJarFileTestCase.class, DataTypes.class, DataTypes.class, Grade.class);
            war.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
            ear.addAsModule(war);
        }
        return ear;
    }


    @Inject
    private CdiTestBean bean;

    @Test
    public void testEntityInMainArchive() throws NamingException {

        bean.createData();
    }

}
