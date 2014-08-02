/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.txtimeout;


import javax.ejb.EJBTransactionRolledbackException;
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
 * Demonstrate concurrency issues that occur when Transaction times out while application or persistence provider is using EntityManager.
 *
 * This test runs until it fails.
 * Each iteration uses a new stateful bean instance, jta transaction, (transaction scoped) entity manager instance.
 *
 * To run this test:
 *
 * 1. Checkout https://github.com/scottmarlow/wildfly/tree/transactiontimeout
 * 2. build.sh clean install -Dmaven.test.skip=true
 * 3. cd testsuite/integration/basic
 * 4. mvn clean install -Dtest=org.jboss.as.test.integration.jpa.txtimeout.TxTimeoutTestCase -Dorg.jboss.remoting-jmx.timeout=600
 *
 * Copy of output logs are available at https://dl.dropboxusercontent.com/u/35343318/txtimeout/txtimeoutserver.zip
 *
 * Scroll to see exception at bottom that shows:
 *   javax.ejb.EJBException: javax.persistence.PersistenceException:
 * Go up a few lines to the start of transaction "0:ffff7f000001:366134e6:539f5abd:17e3" and note the
 * DataSource warning about about "Lock owned during cleanup".
 *
 * If you scroll up and look at previous exceptions, you can see that the exception is normally javax.ejb.EJBTransactionRolledbackException.
 * The last one is not javax.ejb.EJBTransactionRolledbackException.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class TxTimeoutTestCase {

    private static final String ARCHIVE_NAME = "jpa_txTimeoutTest";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, "ejbjar.jar");

        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addClasses(TxTimeoutTestCase.class,
                SFSB1.class,
                Employee.class
        );
        ejbjar.addAsManifestResource(TxTimeoutTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        ear.addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi export \n"), "MANIFEST.MF");
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

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
        } catch (NamingException e) {
            dumpJndi("");
            throw e;
        }
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
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

    /**
     * Loop until exception is thrown that is not EJBTransactionRolledbackException.  This helps to demonstrate
     * what applications see when the JTA transaction is timed out from a background thread.
     *
     * @throws Exception
     */
    @Test
     public void test_UntilTxTimeoutCausesUnexpectedError() throws Exception {
        SFSB1 sfsb1 = lookup("ejbjar/SFSB1", SFSB1.class);
        int count = 0;
        // create 1000 entities in database for testing
        for(int looper = 0; looper < 1000; looper++) {
            sfsb1.createEmployee("name" + looper, "Address"+looper, looper);
        }

        boolean notRolledBackException = false;
        while( !notRolledBackException) {
            try {
                System.out.println("repeating invocation, count=" + count);
                count++;
                sfsb1.getEmployeeUntilTxTimeout();
            }
            catch(Exception exception) {
                if(exception instanceof EJBTransactionRolledbackException) {
                    System.out.println("ignoring expected RollbackException by repeating invocation, count=" + count);
                    sfsb1 = lookup("ejbjar/SFSB1", SFSB1.class);
                }
                else {
                    System.out.println("caught exception that we didn't expect: " + exception.getClass().getName() + ", " + exception.getMessage() +
                            ", count=" +count);

                    notRolledBackException = true;
                }
                // try again until its not RollbackException:
            }
        }

        }

}
