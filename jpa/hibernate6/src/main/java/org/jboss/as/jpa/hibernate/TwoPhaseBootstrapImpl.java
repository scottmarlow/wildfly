/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * TwoPhaseBootstrapImpl
 *
 * @author Scott Marlow
 */
public class TwoPhaseBootstrapImpl implements EntityManagerFactoryBuilder {

    final org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    public TwoPhaseBootstrapImpl(final PersistenceUnitInfo info, final Map map) {
        entityManagerFactoryBuilder =
                    Bootstrap.getEntityManagerFactoryBuilder(info, map, getClassLoaderService(info));
    }

    private ClassLoaderService getClassLoaderService(PersistenceUnitInfo info) {
        PersistenceUnitMetadata persistenceUnitMetadata = (PersistenceUnitMetadata) info;
        return new ClassLoaderServiceImpl(persistenceUnitMetadata.getClassLoaders(), TcclLookupPrecedence.AFTER);
    }

    @Override
    public EntityManagerFactory build() {
        return entityManagerFactoryBuilder.build();
    }

    @Override
    public void cancel() {
        entityManagerFactoryBuilder.cancel();
    }

    @Override
    public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory) {
        entityManagerFactoryBuilder.withValidatorFactory(validatorFactory);
        return this;
    }

}
