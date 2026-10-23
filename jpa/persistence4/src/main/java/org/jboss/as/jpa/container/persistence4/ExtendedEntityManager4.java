/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import java.io.Serial;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.sql.ResultSetMapping;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.ExtendedEntityManager;

/**
 * ExtendedEntityManager subclass that implements the EntityManager methods added in Jakarta Persistence 3.2 and 4.0.
 */
public final class ExtendedEntityManager4 extends ExtendedEntityManager {

    @Serial
    private static final long serialVersionUID = 6979033295500516481L;

    private transient volatile EntityManager4Adapter adapter;

    ExtendedEntityManager4(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        super(puScopedName, underlyingEntityManager, synchronizationType, transactionSynchronizationRegistry, transactionManager);
    }

    private EntityManager4Adapter getAdapter() {
        if (adapter == null) {
            adapter = new EntityManager4Adapter(getAdaptee());
        }
        return adapter;
    }

    private EntityManager4Adapter.Adaptee getAdaptee() {
        return new EntityManager4Adapter.Adaptee() {
            @Override
            public EntityManager getEntityManager() {
                return ExtendedEntityManager4.this.getEntityManager();
            }

            @Override
            public String getClassName(Object entity) {
                return ExtendedEntityManager4.getClassName(entity);
            }

            @Override
            public String getLockModeAsString(LockModeType lockMode) {
                return ExtendedEntityManager4.getLockModeAsString(lockMode);
            }

            @Override
            public boolean isTraceEnabled() {
                return ExtendedEntityManager4.this.isTraceEnabled();
            }

            @Override
            public <T> TypedQuery<T> detachTypedQueryNonTxInvocation(EntityManager underlyingEntityManager, TypedQuery<T> underLyingQuery) {
                return ExtendedEntityManager4.this.detachTypedQueryNonTxInvocation(underlyingEntityManager, underLyingQuery);
            }

            @Override
            public void detachNonTxInvocation(EntityManager underlyingEntityManager) {
                ExtendedEntityManager4.this.detachNonTxInvocation(underlyingEntityManager);
            }

            @Override
            public void transactionIsRequired() {
                ExtendedEntityManager4.this.transactionIsRequired();
            }
        };
    }

    // Methods NEW in Jakarta Persistence 3.2 (delegate to adapter)
    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return getAdapter().createQuery(selectQuery);
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return getAdapter().createQuery(reference);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return getAdapter().find(entityClass, primaryKey, options);
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return getAdapter().find(entityGraph, primaryKey, options);
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        getAdapter().lock(entity, lockMode, options);
    }

    public CacheRetrieveMode getCacheRetrieveMode() {
        return getAdapter().getCacheRetrieveMode();
    }

    public CacheStoreMode getCacheStoreMode() {
        return getAdapter().getCacheStoreMode();
    }

    public <T> T getReference(T entity) {
        return getAdapter().getReference(entity);
    }

    public void refresh(Object entity, RefreshOption... options) {
        getAdapter().refresh(entity, options);
    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return getAdapter().callWithConnection(function);
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        getAdapter().runWithConnection(action);
    }

    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        getAdapter().setCacheRetrieveMode(cacheRetrieveMode);
    }

    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        getAdapter().setCacheStoreMode(cacheStoreMode);
    }

    // Methods NEW in Jakarta Persistence 4.0 (delegate to underlying EM, not in base or adapter)
    public <T> T get(Class<T> entityClass, Object primaryKey) {
        return getEntityManager().get(entityClass, primaryKey);
    }

    public <T> T get(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return getEntityManager().get(entityClass, primaryKey, options);
    }

    public <T> T get(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return getEntityManager().get(entityGraph, primaryKey, options);
    }

    public <T> List<T> getMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return getEntityManager().getMultiple(entityClass, primaryKeys, options);
    }

    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return getEntityManager().getMultiple(entityGraph, primaryKeys, options);
    }

    public <T> List<T> findMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return getEntityManager().findMultiple(entityClass, primaryKeys, options);
    }

    public <T> List<T> findMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return getEntityManager().findMultiple(entityGraph, primaryKeys, options);
    }

    public Statement createStatement(String statement) {
        return getEntityManager().createStatement(statement);
    }

    public Statement createStatement(CriteriaStatement<?> criteriaStatement) {
        return getEntityManager().createStatement(criteriaStatement);
    }

    public Statement createStatement(StatementReference reference) {
        return getEntityManager().createStatement(reference);
    }

    public Statement createNamedStatement(String name) {
        return getEntityManager().createNamedStatement(name);
    }

    public Statement createNativeStatement(String sql) {
        return getEntityManager().createNativeStatement(sql);
    }

    public <T> TypedQuery<T> createNativeQuery(String sqlString, ResultSetMapping<T> resultSetMapping) {
        return getEntityManager().createNativeQuery(sqlString, resultSetMapping);
    }

    public <T> TypedQuery<T> createQuery(String qlString, EntityGraph<T> entityGraph) {
        return getEntityManager().createQuery(qlString, entityGraph);
    }

    public Statement createQuery(CriteriaStatement<?> criteriaStatement) {
        return getEntityManager().createQuery(criteriaStatement);
    }

    public <T> EntityGraph<T> getEntityGraph(Class<T> entityClass, String graphName) {
        return getEntityManager().getEntityGraph(entityClass, graphName);
    }

    public void addOption(EntityManager.Option option) {
        getEntityManager().addOption(option);
    }

    public Set<EntityManager.Option> getOptions() {
        return getEntityManager().getOptions();
    }
}
