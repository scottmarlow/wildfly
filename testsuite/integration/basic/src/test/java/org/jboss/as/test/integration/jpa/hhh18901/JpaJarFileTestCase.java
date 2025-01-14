/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hhh18901;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jpa.jarfile.JarFileEntity;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "hhh18901.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(JarFileEntity.class);
        jar.addClass(Employee.class);
        jar.addClass(Project.class);
        jar.addClass(PartTimeEmployee.class);
        jar.addClass(Department.class);
        jar.addClass(AbstractPersonnel.class);
        jar.addClass(MainArchiveEntity.class);
        jar.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(jar);

        // With change to only loop twice through this code, the failure doesn't occur on first run but always on second run.
        // For more consistent failures we can probably loop a bit more.
        for (int looper = 1 ; looper < 4; looper++ ) {
            JavaArchive clientModule = ShrinkWrap.create(JavaArchive.class,looper + "-notappclientcontainer.jar");
            clientModule.addClasses(JpaJarFileTestCase.class, JpaTestSlsb.class);
            clientModule.addAsManifestResource( JpaJarFileTestCase.class.getPackage(), "application-client.xml","application-client.xml");
            clientModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(clientModule);
        }

        for (int looper = 1 ; looper < 5; looper++ ) {
            JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class,looper + "-ejb-module.jar");
            ejbModule.addClasses(JpaJarFileTestCase.class, JpaTestSlsb.class);
            ejbModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(ejbModule);
        }

        for (int looper = 1 ; looper < 5; looper++ ) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, looper + "-NonTransactionalEmTestCase.war");
            war.addClasses(HttpRequest.class, SimpleServlet.class);
            // war.addAsResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
            war.addAsWebInfResource(JpaJarFileTestCase.class.getPackage(), "web.xml", "web.xml");
            war.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
            ear.addAsModule(war);
        }
        return ear;
    }

    public java.sql.Date getSQLDate(final String sDate) {
      Date d = java.sql.Date.valueOf(sDate);
      return d;
    }

    public Calendar getCalDate() {
      return Calendar.getInstance();
    }


    public java.sql.Date getSQLDate(final int yy, final int mm, final int dd) {
      Calendar newCal = getCalDate();
      newCal.clear();
      newCal.set(yy, mm, dd);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String sDate = sdf.format(newCal.getTime());
      return java.sql.Date.valueOf(sDate);
    }

    public java.sql.Date getSQLDate() {
      Calendar calDate = getCalDate();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String sDate = sdf.format(calDate.getTime());
      java.sql.Date d = java.sql.Date.valueOf(sDate);
      return d;
    }

    @Test
    public void testEntityInMainArchive() throws NamingException {
        PartTimeEmployee[] ptRef = new PartTimeEmployee[5];

         Date d1 = getSQLDate(2000, 2, 14);

         Date d2 = getSQLDate(2001, 6, 27);

         Date d3 = getSQLDate(2002, 7, 7);

         Date d4 = getSQLDate(2003, 3, 3);

         Date d5 = getSQLDate(2004, 4, 10);

         Date d6 = getSQLDate(2005, 2, 18);

         Date d7 = getSQLDate(2000, 9, 17);

         Date d8 = getSQLDate(2001, 11, 14);

         Date d9 = getSQLDate(2002, 10, 4);

         Date d10 = getSQLDate(2003, 1, 25);

        // JpaTestSlsb slsb = (JpaTestSlsb) new InitialContext().lookup("java:module/" + JpaTestSlsb.class.getSimpleName());
        // slsb.testMainArchiveEntity();
    }

    @Test
    public void testEntityInJarFileArchive() throws NamingException {
        // JpaTestSlsb slsb = (JpaTestSlsb) new InitialContext().lookup("java:module/" + JpaTestSlsb.class.getSimpleName());
        // slsb.testJarFileEntity();
    }

}
