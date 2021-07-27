/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.as.jpa.container;

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;

/**
 * LazyEntityManagerFactory
 *
 * @author Scott Marlow
 */
public class LazyEntityManagerFactory implements EntityManagerFactory {

    private volatile EntityManagerFactory delegate;
    private volatile Runnable deferStartOfPersistenceUnit;
    private volatile PersistenceUnitServiceImpl backReference;

    private EntityManagerFactory getDelegate() {
        EntityManagerFactory result = delegate;
        if (result == null) {
            synchronized (this) {
                result = delegate;
                if ( result == null) {
                    try {
                        deferStartOfPersistenceUnit.run();
                        delegate = result = backReference.getEntityManagerFactory();
                    } catch (Throwable deploymentFailureEquivalent) {
                        throw new RuntimeException(deploymentFailureEquivalent.getMessage(), deploymentFailureEquivalent);
                    }
                }
            }
        }
        return result;
    }

    public boolean wasLazyEntityManagerFactoryUsed() {
        return delegate != null;
    }

    public LazyEntityManagerFactory(Runnable task, PersistenceUnitServiceImpl persistenceUnitService) {
        this.backReference = persistenceUnitService;
        deferStartOfPersistenceUnit = task;
    }

    @Override
    public EntityManager createEntityManager() {
        return getDelegate().createEntityManager();
    }

    @Override
    public EntityManager createEntityManager(Map map) {
        return getDelegate().createEntityManager(map);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return getDelegate().createEntityManager(synchronizationType);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return getDelegate().createEntityManager(synchronizationType, map);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return getDelegate().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return getDelegate().getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return getDelegate().isOpen();
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    public Map<String, Object> getProperties() {
        return getDelegate().getProperties();
    }

    @Override
    public Cache getCache() {
        return getDelegate().getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return getDelegate().getPersistenceUnitUtil();
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        getDelegate().addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return getDelegate().unwrap(cls);
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        getDelegate().addNamedEntityGraph(graphName, entityGraph);
    }
}
