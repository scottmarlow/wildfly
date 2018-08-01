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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Enable Hibernate bytecode transformer for application with jboss-deployment-structure.xml
 */
@RunWith(Arquillian.class)
public class VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase
        extends AbstractVerifyHibernate51CompatibilityTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase.class.getName() + ".ear");
        ear.addAsLibraries(getLib());

        WebArchive war = getWar();
        war.addClasses(VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase.class);
        ear.addAsModule(war);

        ear.addAsManifestResource(VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase.class.getPackage(),
                "jboss-deployment-structure.xml","jboss-deployment-structure.xml");
        return ear;
    }
}
