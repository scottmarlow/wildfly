/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.compat.jpa.hibernate.transformer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Enable Hibernate bytecode transformer globally with system-property Hibernate51CompatibilityTransformer=true
 * and also enable it in application with jboss-deployment-structure.xml.
 *
 * It should work, classes should be transformed only once (this test doesn't check it though)
 */
@RunWith(Arquillian.class)
@ServerSetup(VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase.EnableHibernateBytecodeTransformerSetupTask.class)
public class VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase
        extends AbstractVerifyHibernate51CompatibilityTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase.class.getName() + ".ear");
        ear.addAsLibraries(getLib());

        WebArchive war = getWar();
        war.addClasses(VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase.class);
        ear.addAsModule(war);

        ear.addAsManifestResource(VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase.class.getPackage(),
                "jboss-deployment-structure.xml","jboss-deployment-structure.xml");
        return ear;
    }

    public static class EnableHibernateBytecodeTransformerSetupTask implements ServerSetupTask {
        private static final ModelNode PROP_ADDR = new ModelNode()
                .add("system-property", "Hibernate51CompatibilityTransformer");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Operations.createAddOperation(PROP_ADDR);
            op.get("value").set("true");
            managementClient.getControllerClient().execute(op);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Operations.createRemoveOperation(PROP_ADDR);
            managementClient.getControllerClient().execute(op);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }
    }
}
