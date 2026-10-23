/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.sql.ResultSetMapping;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * for JPA 4.0 section 3.8.6
 * used by TransactionScopedEntityManager to detach entities loaded by a query in a non-Jakarta Transactions invocation.
 * This could be a proxy but wrapper classes give faster performance.
 *
 * @author Scott Marlow
 */
public abstract class QueryNonTxInvocationDetacher implements StatementOrTypedQuery {

    private static final QueryNonTxInvocationDetacher.Factory FACTORY; static {
        QueryNonTxInvocationDetacher.Factory f;
        if (WildFlySecurityManager.isChecking()) {
            f = AccessController.doPrivileged((PrivilegedAction<QueryNonTxInvocationDetacher.Factory>) () -> ServiceLoader.load(QueryNonTxInvocationDetacher.Factory.class).iterator().next());
        } else {
            f = ServiceLoader.load(QueryNonTxInvocationDetacher.Factory.class).iterator().next();
        }
        FACTORY = f;
    }

    /**
     * Creates a new {@code QueryNonTxInvocationDetacher}.
     */
    public static Query create(final EntityManager underlyingEntityManager, Query underlyingQuery) {
        return FACTORY.createQueryNonTxInvocationDetacher(underlyingEntityManager, underlyingQuery);
    }

    private final StatementOrTypedQuery underlyingQuery;
    private final EntityManager underlyingEntityManager;

    protected QueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, Query underlyingQuery) {
        this.underlyingQuery = (StatementOrTypedQuery) underlyingQuery;
        this.underlyingEntityManager = underlyingEntityManager;
    }

    @Override
    public List getResultList() {
        List result = underlyingQuery.getResultList();
        /*
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public Object getSingleResult() {
        Object result = underlyingQuery.getSingleResult();
        /*
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public Object getSingleResultOrNull() {
        Object result = underlyingQuery.getSingleResultOrNull();
        /*
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public int executeUpdate() {
        return underlyingQuery.executeUpdate();
    }

    @Override
    public StatementOrTypedQuery setMaxResults(int maxResult) {
        underlyingQuery.setMaxResults(maxResult);
        return this;
    }

    @Override
    public int getMaxResults() {
        return underlyingQuery.getMaxResults();
    }

    @Override
    public StatementOrTypedQuery setFirstResult(int startPosition) {
        underlyingQuery.setFirstResult(startPosition);
        return this;
    }

    @Override
    public int getFirstResult() {
        return underlyingQuery.getFirstResult();
    }

    @Override
    public Map<String, Object> getHints() {
        return underlyingQuery.getHints();
    }

    @Override
    public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(String name, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(String name, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(int position, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(int position, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return underlyingQuery.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return underlyingQuery.getParameter(name);
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        return underlyingQuery.getParameter(name, type);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return underlyingQuery.getParameter(position);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        return underlyingQuery.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        return underlyingQuery.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        return underlyingQuery.getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name) {
        return underlyingQuery.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position) {
        return underlyingQuery.getParameterValue(position);
    }

    @Override
    public StatementOrTypedQuery setFlushMode(FlushModeType flushMode) {
        underlyingQuery.setFlushMode(flushMode);
        return this;
    }

    @Override
    public FlushModeType getFlushMode() {
        return underlyingQuery.getFlushMode();
    }

    @Override
    public StatementOrTypedQuery setLockMode(LockModeType lockMode) {
        underlyingQuery.setLockMode(lockMode);
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        return underlyingQuery.getLockMode();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return underlyingQuery.unwrap(cls);
    }

    // JPA 4.0 Query getters (don't have covariant variants)
    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingQuery.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingQuery.getCacheStoreMode();
    }

    @Override
    public Integer getTimeout() {
        return underlyingQuery.getTimeout();
    }

    @Override
    public QueryFlushMode getQueryFlushMode() {
        return underlyingQuery.getQueryFlushMode();
    }

    // JPA 4.0 Query setters (covariant versions in StatementOrTypedQuery below)
    @Override
    public StatementOrTypedQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public StatementOrTypedQuery setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public StatementOrTypedQuery setTimeout(Integer timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public StatementOrTypedQuery setTimeout(Timeout timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public StatementOrTypedQuery setQueryFlushMode(QueryFlushMode queryFlushMode) {
        underlyingQuery.setQueryFlushMode(queryFlushMode);
        return this;
    }

    @Override
    public StatementOrTypedQuery setParameters(Object... values) {
        underlyingQuery.setParameters(values);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setParameter(String name, P value, Class<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setParameter(String name, P value, Type<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(name, value, converter);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setParameter(int position, P value, Class<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setParameter(int position, P value, Type<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> StatementOrTypedQuery setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(position, value, converter);
        return this;
    }

    // StatementOrTypedQuery-specific methods (JPA 4.0)
    @Override
    public Statement asStatement() {
        return underlyingQuery.asStatement();
    }

    @Override
    public <R> TypedQuery<R> ofType(Class<R> resultClass) {
        return TypedQueryNonTxInvocationDetacher.create(underlyingEntityManager, underlyingQuery.ofType(resultClass));
    }

    @Override
    public <R> TypedQuery<R> withEntityGraph(EntityGraph<R> entityGraph) {
        return TypedQueryNonTxInvocationDetacher.create(underlyingEntityManager, underlyingQuery.withEntityGraph(entityGraph));
    }

    @Override
    public <R> TypedQuery<R> withResultSetMapping(ResultSetMapping<R> resultSetMapping) {
        return TypedQueryNonTxInvocationDetacher.create(underlyingEntityManager, underlyingQuery.withResultSetMapping(resultSetMapping));
    }

    // Covariant overrides that return StatementOrTypedQuery
    @Override
    public StatementOrTypedQuery setHint(String hintName, Object value) {
        underlyingQuery.setHint(hintName, value);
        return this;
    }

    @Override
    public <T> StatementOrTypedQuery setParameter(Parameter<T> param, T value) {
        underlyingQuery.setParameter(param, value);
        return this;
    }

    @Override
    public StatementOrTypedQuery setParameter(String name, Object value) {
        underlyingQuery.setParameter(name, value);
        return this;
    }

    @Override
    public StatementOrTypedQuery setParameter(int position, Object value) {
        underlyingQuery.setParameter(position, value);
        return this;
    }

    public interface Factory {
        QueryNonTxInvocationDetacher createQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, Query underlyingQuery);
    }
}
