package org.jboss.as.test.integration.jpa.transaction.delist;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.ejb.EJB;
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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * delistRegressionTestCase
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class DelistRegressionTestCase {

    private static final String ARCHIVE_NAME = "jpa_delistTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(DelistRegressionTestCase.class, Employee.class, SFSB1.class
        );
        jar.addAsManifestResource(DelistRegressionTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @EJB(mappedName = "java:module/SFSB1!org.jboss.as.test.integration.jpa.transaction.delist.SFSB1")
    private SFSB1 sfsb1;

    @Test
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        try {
            sfsb1.createTwoEmployees("name1", "address1", 1001, "name2", "address2", 1002);
            fail("expected to catch EJBTransactionRolledbackException");
        } catch(EJBTransactionRolledbackException expected) {

        }
        assertNull("first entity should not of been written to the database",sfsb1.getEmployee(1001));
        assertNull("second entity should not of been written to the database",sfsb1.getEmployee(1002));
    }

}
