/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.StatementReference;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.sql.ResultSetMapping;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;

public final class TransactionScopedEntityManager4 extends TransactionScopedEntityManager {

    @Serial
    private static final long serialVersionUID = -713531744224336906L;

    private transient volatile EntityManager4Adapter adapter;


    TransactionScopedEntityManager4(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        super(puScopedName, properties, emf, synchronizationType, transactionSynchronizationRegistry, transactionManager);
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
                return TransactionScopedEntityManager4.this.getEntityManager();
            }

            @Override
            public String getClassName(Object entity) {
                return TransactionScopedEntityManager4.getClassName(entity);
            }

            @Override
            public String getLockModeAsString(LockModeType lockMode) {
                return TransactionScopedEntityManager4.getLockModeAsString(lockMode);
            }

            @Override
            public boolean isTraceEnabled() {
                return TransactionScopedEntityManager4.this.isTraceEnabled();
            }

            @Override
            public <T> TypedQuery<T> detachTypedQueryNonTxInvocation(EntityManager underlyingEntityManager, TypedQuery<T> underLyingQuery) {
                return TransactionScopedEntityManager4.this.detachTypedQueryNonTxInvocation(underlyingEntityManager, underLyingQuery);
            }

            @Override
            public void detachNonTxInvocation(EntityManager underlyingEntityManager) {
                TransactionScopedEntityManager4.this.detachNonTxInvocation(underlyingEntityManager);
            }

            @Override
            public void transactionIsRequired() {
                TransactionScopedEntityManager4.this.transactionIsRequired();
            }
        };
    }

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

    public <T> T get(Class<T> entityClass, Object primaryKey) {
        EntityManager underlyingEntityManager = getEntityManager();
        T result = underlyingEntityManager.get(entityClass, primaryKey);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> T get(Class<T> entityClass, Object primaryKey, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        T result = underlyingEntityManager.get(entityClass, primaryKey, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> T get(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        T result = underlyingEntityManager.get(entityGraph, primaryKey, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> List<T> getMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        List<T> result = underlyingEntityManager.getMultiple(entityClass, primaryKeys, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        List<T> result = underlyingEntityManager.getMultiple(entityGraph, primaryKeys, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> List<T> findMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        List<T> result = underlyingEntityManager.findMultiple(entityClass, primaryKeys, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
    }

    public <T> List<T> findMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        EntityManager underlyingEntityManager = getEntityManager();
        List<T> result = underlyingEntityManager.findMultiple(entityGraph, primaryKeys, options);
        detachNonTxInvocation(underlyingEntityManager);
        return result;
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

    public StatementOrTypedQuery createQuery(String qlString) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachStatementOrTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createQuery(qlString));
    }

    public StatementOrTypedQuery createNamedQuery(String name) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachStatementOrTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createNamedQuery(name));
    }

    public StatementOrTypedQuery createNativeQuery(String sqlString) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachStatementOrTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createNativeQuery(sqlString));
    }

    public StatementOrTypedQuery createNativeQuery(String sqlString, String resultSetMapping) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachStatementOrTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createNativeQuery(sqlString, resultSetMapping));
    }

    @SuppressWarnings("unchecked")
    public TypedQuery createNativeQuery(String sqlString, Class resultClass) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createNativeQuery(sqlString, resultClass));
    }

    public <T> TypedQuery<T> createNativeQuery(String sqlString, ResultSetMapping<T> resultSetMapping) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createNativeQuery(sqlString, resultSetMapping));
    }

    public <T> TypedQuery<T> createQuery(String qlString, EntityGraph<T> entityGraph) {
        EntityManager underlyingEntityManager = getEntityManager();
        return detachTypedQueryNonTxInvocation(underlyingEntityManager, underlyingEntityManager.createQuery(qlString, entityGraph));
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
