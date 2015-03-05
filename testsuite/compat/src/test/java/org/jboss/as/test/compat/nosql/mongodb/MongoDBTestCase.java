package org.jboss.as.test.compat.nosql.mongodb;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MongoDBTestCase
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class MongoDBTestCase {
    private static final String ARCHIVE_NAME = "MongoDBTestCase_test";

@ArquillianResource
   private static InitialContext iniCtx;

   @BeforeClass
   public static void beforeClass() throws NamingException {
       iniCtx = new InitialContext();
   }

   @Deployment
   public static Archive<?> deploy() throws Exception {

       EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
       // addTestJarsToEar(ear);
       JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
       lib.addClasses(StatefulTestBean.class);
       ear.addAsModule(lib);
       final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
       main.addClasses(MongoDBTestCase.class);
       ear.addAsModule(main);
       // ear.addAsManifestResource(new StringAsset("Dependencies: org.mongodb.driver \n"), "MANIFEST.MF");
       return ear;
   }

   protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
       return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!" + interfaceType.getName()));
   }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        StatefulTestBean statefulTestBean = lookup("StatefulTestBean", StatefulTestBean.class);
        statefulTestBean.addUserComment();
        statefulTestBean.addProduct();
    }

}