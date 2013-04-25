package org.jboss.as.jpa.management;

import javax.persistence.EntityManagerFactory;

import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;

/**
 * EntityManagerFactoryLookup
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryLookup implements EntityManagerFactoryAccess {

    @Override
    public EntityManagerFactory entityManagerFactory(final String scopedPersistenceUnitName) {
        return PersistenceUnitRegistryImpl.INSTANCE.getPersistenceUnitService(scopedPersistenceUnitName).getEntityManagerFactory();
    }

}
