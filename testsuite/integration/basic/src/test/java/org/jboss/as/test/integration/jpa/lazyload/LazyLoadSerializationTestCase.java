/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jpa.lazyload;

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Serialized + deserialized lazy loaded entity can be accessed.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class LazyLoadSerializationTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(LazyLoadSerializationTestCase.class,
                Employee.class,
                Company.class,
                SFSB1.class
        );
        jar.addAsManifestResource(LazyLoadSerializationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private static InitialContext iniCtx;


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testEmployee() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        // tx1 will create the employee
        sfsb1.createEmployee("Sally", "1 home street", 1);

        // non-tx2 will load the entity
        Employee emp2 = sfsb1.getEmployee(1);
        assertTrue("Company is set " + emp2.getCompany(), emp2.getCompany() != null);

        assertTrue(
                "Test that Employee was serialized and deserialized and employee name is still Sally: " + emp2.getName(),
                "Sally".equals(emp2.getName()));
    }

}
