/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;
import org.jboss.as.jpa.container.TypedQueryNonTxInvocationDetacher;

public class TypedQueryNonTxInvocationDetacher4<X> extends TypedQueryNonTxInvocationDetacher<X> {

    private final EntityManager underlyingEntityManager;
    private final TypedQuery<X> underlyingQuery;

    protected TypedQueryNonTxInvocationDetacher4(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingQuery = underlyingQuery;
    }

    @Override
    public X getSingleResultOrNull() {
        X result = (X)underlyingQuery.getSingleResultOrNull();
        /*
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public TypedQuery<X> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public TypedQuery<X> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingQuery.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingQuery.getCacheStoreMode();
    }

    @Override
    public TypedQuery<X> setTimeout(Integer timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingQuery.getTimeout();
    }

    @Override
    public TypedQuery<X> setTimeout(Timeout timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public TypedQuery<X> setQueryFlushMode(QueryFlushMode queryFlushMode) {
        underlyingQuery.setQueryFlushMode(queryFlushMode);
        return this;
    }

    @Override
    public QueryFlushMode getQueryFlushMode() {
        return underlyingQuery.getQueryFlushMode();
    }

    @Override
    public TypedQuery<X> setParameters(Object... values) {
        underlyingQuery.setParameters(values);
        return this;
    }

    @Override
    public TypedQuery<X> setLockScope(PessimisticLockScope lockScope) {
        underlyingQuery.setLockScope(lockScope);
        return this;
    }

    @Override
    public PessimisticLockScope getLockScope() {
        return underlyingQuery.getLockScope();
    }

    @Override
    public long getResultCount() {
        return underlyingQuery.getResultCount();
    }

    @Override
    public TypedQuery<X> addOption(TypedQuery.Option option) {
        underlyingQuery.addOption(option);
        return this;
    }

    @Override
    public Set<TypedQuery.Option> getOptions() {
        return underlyingQuery.getOptions();
    }

    @Override
    public <P> TypedQuery<X> setParameter(String name, P value, Class<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> TypedQuery<X> setParameter(String name, P value, Type<P> type) {
        underlyingQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> TypedQuery<X> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(name, value, converter);
        return this;
    }

    @Override
    public <P> TypedQuery<X> setParameter(int position, P value, Class<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> TypedQuery<X> setParameter(int position, P value, Type<P> type) {
        underlyingQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> TypedQuery<X> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingQuery.setConvertedParameter(position, value, converter);
        return this;
    }
}
