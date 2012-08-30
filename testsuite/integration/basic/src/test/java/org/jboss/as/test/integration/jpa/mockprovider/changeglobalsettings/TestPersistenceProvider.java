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

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * TestPersistenceProvider
 *
 * @author Scott Marlow
 */
public class TestPersistenceProvider implements PersistenceProvider {

    private static volatile PersistenceUnitInfo lastPersistenceUnitInfo = null;

    public static PersistenceUnitInfo getLastPersistenceUnitInfo() {
        return lastPersistenceUnitInfo;
    }

    public static void clearLastPersistenceUnitInfo() {
        lastPersistenceUnitInfo = null;
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        System.out.println("TestPersistenceProvider.createEntityManagerFactory()");
        return null;
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        System.out.println("TestPersistenceProvider.createContainerEntityManagerFactory()");
        lastPersistenceUnitInfo = info;

        TestEntityManagerFactory testEntityManagerFactory =
            new TestEntityManagerFactory();
        Class[] targetInterfaces = EntityManagerFactory.class.getInterfaces();
        Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
        boolean alreadyHasInterfaceClass = false;
        for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
            Class interfaceClass =  targetInterfaces[interfaceIndex];
            if (interfaceClass.equals(EntityManagerFactory.class)) {
                proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                alreadyHasInterfaceClass = true;
                break;
            }
            proxyInterfaces[1 + interfaceIndex] = interfaceClass;
        }
        if (!alreadyHasInterfaceClass) {
            proxyInterfaces[0] = EntityManagerFactory.class;
        }

        EntityManagerFactory proxyEntityManagerFactory = (EntityManagerFactory)Proxy.newProxyInstance(
                testEntityManagerFactory.getClass().getClassLoader(), //use the target classloader so the proxy has the same scope
                proxyInterfaces,
                testEntityManagerFactory
        );

        System.out.println("TestPersistenceProvider.createContainerEntityManagerFactory() is returning " + proxyEntityManagerFactory);
        return proxyEntityManagerFactory;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        System.out.println("TestPersistenceProvider.getProviderUtil()");
        return null;
    }
}
