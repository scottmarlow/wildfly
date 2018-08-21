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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceProvider;

import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * EntityManagerFactoryProxy prototype for lazy initializing EntityManagerFactory
 * <p>
 * TODO: use ASM or Bytebuddy for creating the proxy.
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryProxy implements InvocationHandler {
    private Object lazyEntityManagerFactory = null;
    private final PersistenceProvider persistenceProvider;
    private final PersistenceUnitMetadata pu;
    private final Map properties;
    private boolean isOpen = true;

    public EntityManagerFactoryProxy(PersistenceProvider persistenceProvider, PersistenceUnitMetadata pu, Map properties) {
        this.persistenceProvider = persistenceProvider;
        this.pu = pu;
        this.properties = properties;
    }

    public static EntityManagerFactory create(PersistenceProvider persistenceProvider, final PersistenceUnitMetadata pu, final Map properties) {

        return (EntityManagerFactory) Proxy.newProxyInstance(EntityManagerFactory.class.getClassLoader(), new Class<?>[]{EntityManagerFactory.class},
                new EntityManagerFactoryProxy(persistenceProvider, pu, properties));
    }

    public boolean createdEntityManagerFactory() {
        return lazyEntityManagerFactory != null;
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (lazyEntityManagerFactory == null) {
            boolean timeToCreateEntityManagerFactory = false;
            if ("toString".equals(method.getName()) &&
                    "string".equals(method.getGenericReturnType().getTypeName())) {
                return "uninitialized proxy for " + pu.getScopedPersistenceUnitName();
            } else if ("isOpen".equals(method.getName()) &&
                    "boolean".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                return isOpen;
            } else if ("close".equals(method.getName()) &&
                    "void".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                isOpen = false;
                return null;
            } else if ("getCriteriaBuilder".equals(method.getName()) &&
                    "javax.persistence.criteria.CriteriaBuilder".equals(method.getGenericReturnType().getTypeName())) {
                timeToCreateEntityManagerFactory = true;
            } else if ("getMetamodel".equals(method.getName()) &&
              "javax.persistence.metamodel.Metamodel".equals(method.getGenericReturnType().getTypeName())) {
                timeToCreateEntityManagerFactory = true;
            } else if ("createEntityManager".equals(method.getName())) {
                SynchronizationType synchronizationType = null;
                Map properties = null;
                // handle various forms of createEntityManager
                // public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map);
                // public EntityManager createEntityManager(SynchronizationType synchronizationType);
                // public EntityManager createEntityManager(Map map);
                // public EntityManager createEntityManager();
                if (args != null) {
                    for ( Object arg : args) {
                        if (arg instanceof Map) {
                            properties = (Map)arg;
                        } else if (arg instanceof SynchronizationType) {
                            synchronizationType = (SynchronizationType)arg;
                        }
                    }
                }
                return EntityManagerProxy.create(this, synchronizationType, properties);
            }

            if (timeToCreateEntityManagerFactory) {
                if (!isOpen) {
                    throw new IllegalStateException("EntityManagerFactory is closed");
                }
                createEntityManagerFactory();
            }
        }
        return method.invoke(lazyEntityManagerFactory, args);
    }

    protected void createEntityManagerFactory() {
        synchronized (this) {
            if (lazyEntityManagerFactory == null) {
                lazyEntityManagerFactory = persistenceProvider.createContainerEntityManagerFactory(pu, properties);
            }
        }
    }

    protected EntityManager createEntityManager(SynchronizationType synchronizationType, Map properties) {
        return ((EntityManagerFactory)lazyEntityManagerFactory).createEntityManager(synchronizationType, properties);
    }

}
