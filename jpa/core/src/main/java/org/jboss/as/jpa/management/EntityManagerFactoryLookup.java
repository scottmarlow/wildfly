package org.jboss.as.jpa.management;

import javax.persistence.EntityManagerFactory;

import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jipijapa.spi.statistics.EntityManagerFactoryAccess;

/**
 * EntityManagerFactoryLookup
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryLookup implements EntityManagerFactoryAccess {

    final String scopedPersistenceUnitName;
    public EntityManagerFactoryLookup(final String scopedPersistenceUnitName) {
        this.scopedPersistenceUnitName = scopedPersistenceUnitName;
    }

    @Override
    public EntityManagerFactory entityManagerFactory() {
        return PersistenceUnitRegistryImpl.INSTANCE.getPersistenceUnitService(scopedPersistenceUnitName).getEntityManagerFactory();
    }

    @Override
    public String getScopedPersistenceUnitName() {
        return scopedPersistenceUnitName;
    }
}
