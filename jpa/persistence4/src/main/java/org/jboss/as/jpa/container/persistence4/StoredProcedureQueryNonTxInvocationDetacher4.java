/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import java.util.List;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.sql.ResultSetMapping;
import org.jboss.as.jpa.container.StoredProcedureQueryNonTxInvocationDetacher;

public class StoredProcedureQueryNonTxInvocationDetacher4 extends StoredProcedureQueryNonTxInvocationDetacher {

    private final EntityManager underlyingEntityManager;
    private final StoredProcedureQuery underlyingStoredProcedureQuery;

    protected StoredProcedureQueryNonTxInvocationDetacher4(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingStoredProcedureQuery) {
        super(underlyingEntityManager, underlyingStoredProcedureQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingStoredProcedureQuery = underlyingStoredProcedureQuery;
    }

    @Override
    public Object getSingleResultOrNull() {
        try {
            return underlyingStoredProcedureQuery.getSingleResultOrNull();
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public StoredProcedureQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingStoredProcedureQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingStoredProcedureQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingStoredProcedureQuery.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingStoredProcedureQuery.getCacheStoreMode();
    }

    @Override
    public StoredProcedureQuery setTimeout(Integer timeout) {
        underlyingStoredProcedureQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingStoredProcedureQuery.getTimeout();
    }

    @Override
    public StoredProcedureQuery setTimeout(Timeout timeout) {
        underlyingStoredProcedureQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public StoredProcedureQuery setQueryFlushMode(QueryFlushMode queryFlushMode) {
        underlyingStoredProcedureQuery.setQueryFlushMode(queryFlushMode);
        return this;
    }

    @Override
    public QueryFlushMode getQueryFlushMode() {
        return underlyingStoredProcedureQuery.getQueryFlushMode();
    }

    @Override
    public StoredProcedureQuery setParameters(Object... values) {
        underlyingStoredProcedureQuery.setParameters(values);
        return this;
    }

    @Override
    public StoredProcedureQuery addOption(StoredProcedureQuery.Option option) {
        underlyingStoredProcedureQuery.addOption(option);
        return this;
    }

    @Override
    public Set<StoredProcedureQuery.Option> getOptions() {
        return underlyingStoredProcedureQuery.getOptions();
    }

    @Override
    public void close() {
        underlyingStoredProcedureQuery.close();
    }

    @Override
    public <P> StoredProcedureQuery setParameter(String name, P value, Class<P> type) {
        underlyingStoredProcedureQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> StoredProcedureQuery setParameter(String name, P value, Type<P> type) {
        underlyingStoredProcedureQuery.setParameter(name, value, type);
        return this;
    }

    @Override
    public <P> StoredProcedureQuery setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingStoredProcedureQuery.setConvertedParameter(name, value, converter);
        return this;
    }

    @Override
    public <P> StoredProcedureQuery setParameter(int position, P value, Class<P> type) {
        underlyingStoredProcedureQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> StoredProcedureQuery setParameter(int position, P value, Type<P> type) {
        underlyingStoredProcedureQuery.setParameter(position, value, type);
        return this;
    }

    @Override
    public <P> StoredProcedureQuery setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
        underlyingStoredProcedureQuery.setConvertedParameter(position, value, converter);
        return this;
    }

    @Override
    public <R> List<R> getResultList(Class<R> type) {
        try {
            return underlyingStoredProcedureQuery.getResultList(type);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <R> List<R> getResultList(ResultSetMapping<R> resultSetMapping) {
        try {
            return underlyingStoredProcedureQuery.getResultList(resultSetMapping);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <R> R getSingleResult(Class<R> type) {
        try {
            return underlyingStoredProcedureQuery.getSingleResult(type);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <R> R getSingleResult(ResultSetMapping<R> resultSetMapping) {
        try {
            return underlyingStoredProcedureQuery.getSingleResult(resultSetMapping);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <R> R getSingleResultOrNull(Class<R> type) {
        try {
            return underlyingStoredProcedureQuery.getSingleResultOrNull(type);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <R> R getSingleResultOrNull(ResultSetMapping<R> resultSetMapping) {
        try {
            return underlyingStoredProcedureQuery.getSingleResultOrNull(resultSetMapping);
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public <T> T getOutputParameterValue(Parameter<T> parameter) {
        return underlyingStoredProcedureQuery.getOutputParameterValue(parameter);
    }

    @Override
    public <T> Parameter<T> registerResultParameter(Class<T> type) {
        return underlyingStoredProcedureQuery.registerResultParameter(type);
    }

    @Override
    public <T> Parameter<T> registerParameter(int position, Class<T> type, ParameterMode mode) {
        return underlyingStoredProcedureQuery.registerParameter(position, type, mode);
    }

    @Override
    public <T> Parameter<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
        return underlyingStoredProcedureQuery.registerParameter(name, type, mode);
    }

    @Override
    public <T> Parameter<T> registerConvertedParameter(int position, Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode) {
        return underlyingStoredProcedureQuery.registerConvertedParameter(position, converter, mode);
    }

    @Override
    public <T> Parameter<T> registerConvertedParameter(String name, Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode) {
        return underlyingStoredProcedureQuery.registerConvertedParameter(name, converter, mode);
    }
}
