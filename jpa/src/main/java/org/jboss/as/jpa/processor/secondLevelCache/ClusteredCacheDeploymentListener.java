package org.jboss.as.jpa.processor.secondLevelCache;

import java.util.Properties;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * ClusteredCacheDeploymentListener
 *
 * @author Scott Marlow
 */
public class ClusteredCacheDeploymentListener implements EventListener {

    @Override
    public void beforeEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public void afterEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public Wrapper startCache(Classification cacheType, Properties properties) throws Exception {
        return null;
    }

    @Override
    public void addCacheDependencies(Classification cacheType, Properties properties) {

    }

    @Override
    public void stopCache(Classification cacheType, Wrapper wrapper, boolean skipStop) {

    }
}
