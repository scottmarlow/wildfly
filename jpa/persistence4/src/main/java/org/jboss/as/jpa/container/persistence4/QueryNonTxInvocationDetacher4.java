/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.jboss.as.jpa.container.QueryNonTxInvocationDetacher;

final class QueryNonTxInvocationDetacher4 extends QueryNonTxInvocationDetacher {

    private final EntityManager underlyingEntityManager;
    private final Query underlyingQuery;

    QueryNonTxInvocationDetacher4(EntityManager underlyingEntityManager, Query underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingQuery = underlyingQuery;
    }

    @Override
    public Object getSingleResultOrNull() {
        Object result = underlyingQuery.getSingleResultOrNull();
        /**
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public Query setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingQuery.getCacheRetrieveMode();
    }

    @Override
    public Query setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingQuery.getCacheStoreMode();
    }

    @Override
    public Query setTimeout(Integer timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingQuery.getTimeout();
    }

    @Override
    public Query setTimeout(Timeout timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Query setQueryFlushMode(QueryFlushMode queryFlushMode) {
        underlyingQuery.setQueryFlushMode(queryFlushMode);
        return this;
    }

    @Override
    public QueryFlushMode getQueryFlushMode() {
        return underlyingQuery.getQueryFlushMode();
    }

    @Override
    public Query setParameters(Object... values) {
        underlyingQuery.setParameters(values);
        return this;
    }

    @Override
    public <P> Query setParameter(String name, P value, Class<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> Query setParameter(String name, P value, Type<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> Query setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(name, value, converter);
        return this;
    }

    @Override
    public <P> Query setParameter(int position, P value, Class<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> Query setParameter(int position, P value, Type<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> Query setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(position, value, converter);
        return this;
    }
}
