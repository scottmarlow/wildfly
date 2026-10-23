/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

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
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.sql.ResultSetMapping;
import org.jboss.as.jpa.container.UnsynchronizedEntityManagerWrapper;

final class UnsynchronizedEntityManagerWrapper4 extends UnsynchronizedEntityManagerWrapper {

    private final EntityManager entityManager;

    UnsynchronizedEntityManagerWrapper4(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return entityManager.createQuery(reference);
    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return entityManager.callWithConnection(function);
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        entityManager.runWithConnection(action);
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        entityManager.lock(entity, lockMode, options);
    }

    public void refresh(Object entity, RefreshOption... options) {
        entityManager.refresh(entity, options);
    }

    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return entityManager.createQuery(selectQuery);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return entityManager.find(entityClass, primaryKey, options);
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return entityManager.find(entityGraph, primaryKey, options);
    }

    public CacheRetrieveMode getCacheRetrieveMode() {
        return entityManager.getCacheRetrieveMode();
    }

    public CacheStoreMode getCacheStoreMode() {
        return entityManager.getCacheStoreMode();
    }

    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        entityManager.setCacheRetrieveMode(cacheRetrieveMode);
    }

    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        entityManager.setCacheStoreMode(cacheStoreMode);
    }

    public <T> T getReference(T entity) {
        return entityManager.getReference(entity);
    }

    public <T> T get(Class<T> entityClass, Object primaryKey) {
        return entityManager.get(entityClass, primaryKey);
    }

    public <T> T get(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return entityManager.get(entityClass, primaryKey, options);
    }

    public <T> T get(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return entityManager.get(entityGraph, primaryKey, options);
    }

    public <T> List<T> getMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return entityManager.getMultiple(entityClass, primaryKeys, options);
    }

    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return entityManager.getMultiple(entityGraph, primaryKeys, options);
    }

    public <T> List<T> findMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return entityManager.findMultiple(entityClass, primaryKeys, options);
    }

    public <T> List<T> findMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return entityManager.findMultiple(entityGraph, primaryKeys, options);
    }

    public Statement createStatement(String statement) {
        return entityManager.createStatement(statement);
    }

    public Statement createStatement(CriteriaStatement<?> criteriaStatement) {
        return entityManager.createStatement(criteriaStatement);
    }

    public Statement createStatement(StatementReference reference) {
        return entityManager.createStatement(reference);
    }

    public Statement createNamedStatement(String name) {
        return entityManager.createNamedStatement(name);
    }

    public Statement createNativeStatement(String sql) {
        return entityManager.createNativeStatement(sql);
    }

    public StatementOrTypedQuery createQuery(String qlString) {
        return entityManager.createQuery(qlString);
    }

    public StatementOrTypedQuery createNamedQuery(String name) {
        return entityManager.createNamedQuery(name);
    }

    public StatementOrTypedQuery createNativeQuery(String sqlString) {
        return entityManager.createNativeQuery(sqlString);
    }

    public StatementOrTypedQuery createNativeQuery(String sqlString, String resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    @SuppressWarnings("unchecked")
    public TypedQuery createNativeQuery(String sqlString, Class resultClass) {
        return entityManager.createNativeQuery(sqlString, resultClass);
    }

    public <T> TypedQuery<T> createNativeQuery(String sqlString, ResultSetMapping<T> resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    public <T> TypedQuery<T> createQuery(String qlString, EntityGraph<T> entityGraph) {
        return entityManager.createQuery(qlString, entityGraph);
    }

    public Statement createQuery(CriteriaStatement<?> criteriaStatement) {
        return entityManager.createQuery(criteriaStatement);
    }

    public <T> EntityGraph<T> getEntityGraph(Class<T> entityClass, String graphName) {
        return entityManager.getEntityGraph(entityClass, graphName);
    }

    public void addOption(EntityManager.Option option) {
        entityManager.addOption(option);
    }

    public Set<EntityManager.Option> getOptions() {
        return entityManager.getOptions();
    }
}
