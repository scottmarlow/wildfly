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

package org.jboss.as.test.integration.jpa.secondlevelcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JPA Second level cache tests
 *
 * @author Scott Marlow and Zbynek Roubalik
 */
@RunWith(Arquillian.class)
public class JPA2LCTestCase {

    private static final String ARCHIVE_NAME = "jpa_SecondLevelCacheTestCase";

    // cache region name prefix, use getCacheRegionName() method to get the value!
    private static String CACHE_REGION_NAME = null;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JPA2LCTestCase.class,
                Employee.class,
                SFSB2LC.class
        );

        jar.addAsManifestResource(JPA2LCTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    // Cache region name depends on the internal entity cache naming convention:
    // "fully application scoped persistence unit name" + "the entity class full name"
    // first part could be rewritten by property "hibernate.cache.region_prefix"
    // This method returns prefix + package name, the entity name needs to be appended
    public String getCacheRegionName() throws Exception {

        if (CACHE_REGION_NAME == null) {
            SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
            String prefix = sfsb.getCacheRegionName();

            assertNotNull("'hibernate.cache.region_prefix' is null.", prefix);
            CACHE_REGION_NAME = prefix + '.' + this.getClass().getPackage().getName() + '.';
        }

        return CACHE_REGION_NAME;
    }


    // When entity caching is enabled, loading all entities at once
    // will put all entities in the cache. During the SAME session,
    // when looking up for the ID of an entity which was returned by
    // the original query, no SQL queries should be executed.
    @Test
    @InSequence(3)
    public void testEntityCacheSameSession() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.sameSessionCheck(getCacheRegionName());
        if (!message.equals("OK")) {
            fail(message);
        }
    }
}
