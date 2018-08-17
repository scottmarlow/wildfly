/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2018, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * EntityManagerFactoryProxy prototype for lazy initializing EntityManagerFactory
 *
 * TODO: use ASM or Bytebuddy for creating the proxy.
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryProxy implements InvocationHandler {
    private Object lazyEntityManagerFactory = null;
    private final PersistenceProvider persistenceProvider;
    private final PersistenceUnitMetadata pu;
    private final Map properties;

    public EntityManagerFactoryProxy(PersistenceProvider persistenceProvider, PersistenceUnitMetadata pu, Map properties) {
        this.persistenceProvider = persistenceProvider;
        this.pu = pu;
        this.properties= properties;
    }

    public static EntityManagerFactory create(PersistenceProvider persistenceProvider, final PersistenceUnitMetadata pu, final Map properties) {

        return (EntityManagerFactory)Proxy.newProxyInstance(EntityManagerFactory.class.getClassLoader(), new Class<?>[] { EntityManagerFactory.class },
                new EntityManagerFactoryProxy(persistenceProvider, pu, properties ));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(lazyEntityManagerFactory == null) {
            synchronized (proxy) {
                if(lazyEntityManagerFactory == null) {
                    lazyEntityManagerFactory = persistenceProvider.createContainerEntityManagerFactory(pu, properties);
                }
            }
        }
        return method.invoke(lazyEntityManagerFactory, args);
    }

}
