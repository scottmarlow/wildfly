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

package org.jboss.as.test.integration.jpa.mockprovider.changeglobalsettings;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.spi.PersistenceUnitInfo;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 *
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ChangeGlobalSettingsTestCase.CustomSetup.class)
public class ChangeGlobalSettingsTestCase {

    static class CustomSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            // <subsystem xmlns="urn:jboss:domain:jpa:1.1">
            //  <jpa default-datasource="" default-providerModule="org.hibernate" default-extended-persistence-inheritance="SHALLOW" default-vfs="false"/>
            //  </subsystem>

            final List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();

            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "jpa")).toModelNode());
            op.get(NAME).set("default-vfs");
            op.get(VALUE).set(false);
            updates.add(op);
            applyUpdates(managementClient.getControllerClient(), updates);
        }

        protected static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) {
            for (ModelNode update : updates) {
                try {
                    applyUpdate(client, update, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        protected static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws Exception {
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
                if (result.hasDefined("result")) {
                    System.out.println("xxx " + result.get("result"));
                }
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) {

            ModelNode op = new ModelNode();
            //op.get(OP).set(REMOVE);
            //op.get(OP_ADDR).add(SUBSYSTEM, "security");
            //op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
            // Don't rollback when the AS detects the war needs the module
            //op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

            try {
                applyUpdate(managementClient.getControllerClient(), op, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    };

    private static final String ARCHIVE_NAME = "jpa_ChangeGlobalSettings";
    private static final String MODULE_NAME = "ejbjar";
    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive persistenceProvider = ShrinkWrap.create(JavaArchive.class, "testpersistenceprovider.jar");
        persistenceProvider.addClasses(
                    TestEntityManagerFactory.class,
                    TestPersistenceProvider.class
                );

        // META-INF/services/javax.persistence.spi.PersistenceProvider
        persistenceProvider.addAsResource(new StringAsset("org.jboss.as.test.integration.jpa.mockprovider.changeglobalsettings.TestPersistenceProvider"),
                "META-INF/services/javax.persistence.spi.PersistenceProvider");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addClasses(ChangeGlobalSettingsTestCase.class,
                SFSB1.class,
                StatefulInterface1.class
        );
        ejbjar.addAsManifestResource(ChangeGlobalSettingsTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class, ChangeGlobalSettingsTestCase.class);
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

    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Test
    public void test_persistenceUnitInfoURLS() throws Exception {
        try {
            //         final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
            //                + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            StatefulInterface1 slsb = (StatefulInterface1) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME +
                "/" + SFSB1.class.getSimpleName() + "!" + StatefulInterface1.class.getName());
            PersistenceUnitInfo persistenceUnitInfo = slsb.getLastPersistenceUnitInfo();
            assertNotNull("TestPersistenceProvider.getLastPersistenceUnitInfo() returns expected (non-null) PersistenceUnitInfo",persistenceUnitInfo);
            assertTrue("testing that PersistenceUnitInfo.getPersistenceUnitRootUrl() url is file based, failed because getPersistenceUnitRootUrl is " +
                    persistenceUnitInfo.getPersistenceUnitRootUrl().getProtocol(),
                    "file".equals(TestPersistenceProvider.getLastPersistenceUnitInfo().getPersistenceUnitRootUrl().getProtocol()));
        } finally {
            TestPersistenceProvider.clearLastPersistenceUnitInfo();
        }
    }

}
