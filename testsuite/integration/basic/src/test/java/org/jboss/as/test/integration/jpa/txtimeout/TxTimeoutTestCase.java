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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
@RunAsClient
public class TxTimeoutTestCase {

    private static final String ARCHIVE_NAME = "jpa_txTimeoutTest";

    @ArquillianResource
   	URL baseUrl;

    @Deployment
        public static WebArchive deployment() {
            WebArchive war = ShrinkWrap.create(WebArchive.class, "TxTimeoutTestCase.war");
            war.addClasses(HttpRequest.class, SimpleServlet.class, Employee.class);
            // WEB-INF/classes is implied
            war.addAsResource(TxTimeoutTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");

            return war;
        }

    private String invokeServlet(String urlPattern) throws Exception {
        String request = baseUrl.toString() + urlPattern;
        return HttpRequest.get(request, 60, SECONDS);
    }

    @Test
    public void simulateInvalidApplicationStateAfterTxRollback() throws Exception {
        String result = invokeServlet("simple");
        assertEquals("success", result);

    }

}
