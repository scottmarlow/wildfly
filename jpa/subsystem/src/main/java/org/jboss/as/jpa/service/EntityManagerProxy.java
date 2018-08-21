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
import javax.persistence.SynchronizationType;

/**
 * EntityManagerProxy
 *
 * @author Scott Marlow
 */
public class EntityManagerProxy implements InvocationHandler {

    // note that EntityManager is not required to be thread safe but EntityManagerFactory is
    private Object lazyEntityManager = null;
    private final EntityManagerFactoryProxy entityManagerFactoryProxy;
    private final SynchronizationType synchronizationType;
    private final Map properties;
    private boolean isOpen = true;

    public EntityManagerProxy(EntityManagerFactoryProxy entityManagerFactoryProxy, SynchronizationType synchronizationType, Map properties) {
        this.entityManagerFactoryProxy = entityManagerFactoryProxy;
        this.synchronizationType = synchronizationType;
        this.properties = properties;
    }

    public static EntityManager create(EntityManagerFactoryProxy entityManagerFactoryProxy, SynchronizationType synchronizationType, Map properties) {

        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[]{EntityManager.class},
                new EntityManagerProxy(entityManagerFactoryProxy, synchronizationType, properties));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (!entityManagerFactoryProxy.createdEntityManagerFactory()) {
            // didn't create EntityManagerFactory yet
            if ("toString".equals(method.getName()) &&
                    "string".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                return "uninitialized (EntityManagerFactory) EntityManager proxy";
            } else if ("close".equals(method.getName()) &&
                    "void".equals(method.getGenericReturnType().getTypeName())) {
                isOpen = false;
                return null;
            } else if ("isOpen".equals(method.getName()) &&
                    "boolean".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                return isOpen && entityManagerFactoryProxy.isOpen();
            }

            // for other method calls, hand off EMF creation to the entityManagerFactoryProxy
            entityManagerFactoryProxy.createEntityManagerFactory();

        } else if (lazyEntityManager == null) { // no need to create the EntityManager for toString(), close(), isOpen()
            if ("toString".equals(method.getName()) &&
                    "string".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                return "uninitialized EntityManager proxy";
            } else if ("close".equals(method.getName()) &&
                    "void".equals(method.getGenericReturnType().getTypeName())) {
                isOpen = false;
                return null;
            } else if ("isOpen".equals(method.getName()) &&
                    "boolean".equals(method.getGenericReturnType().getTypeName()) &&
                    method.getGenericParameterTypes().length == 0) {
                return isOpen && entityManagerFactoryProxy.isOpen();
            }
        }


        if (lazyEntityManager == null) {

            if (!entityManagerFactoryProxy.createdEntityManagerFactory()) {
                throw new IllegalStateException("Race condition in lazy EntityManagerFactory creation, " +
                        "cannot create lazy EntityManager");
            }
            if (!entityManagerFactoryProxy.isOpen()) {
                throw new IllegalStateException("EntityManagerFactory is closed");
            }

            lazyEntityManager = entityManagerFactoryProxy.createEntityManager(synchronizationType, properties);
        }

        if ("close".equals(method.getName()) &&
                "void".equals(method.getGenericReturnType().getTypeName())) {
            isOpen = false;
        }
        return method.invoke(lazyEntityManager, args);
    }

}
