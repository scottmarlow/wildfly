/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction.lazyconnectionenlistment;


import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * LazyEnlistmentTestCase
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class LazyEnlistmentTestCase {
    private static final String ARCHIVE_NAME = "jpa_LazyEnlistmentTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(LazyEnlistmentTestCase.class,
                Employee.class,
                TestAPCBean.class,
                ActivityNamesBean.class
        );
        jar.addAsManifestResource(LazyEnlistmentTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @Inject
    private TestAPCBean testAPCBean;

    @Test
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        testAPCBean.startUserTransactionGetConnectionCallOtherBean();
    }

}
