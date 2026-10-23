/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

/**
 * Represents the persistence unit definition
 *
 * @author Scott Marlow
 */
public interface PersistenceUnitMetadata extends PersistenceUnitInfo {
    void setPersistenceUnitName(String name);

    void setScopedPersistenceUnitName(String scopedName);

    String getScopedPersistenceUnitName();

    void setContainingModuleName(ArrayList<String> getContainingModuleName);
    ArrayList<String> getContainingModuleName();

    void setPersistenceProviderClassName(String provider);

    void setJtaDataSource(DataSource jtaDataSource);

    void setNonJtaDataSource(DataSource nonJtaDataSource);

    void setJtaDataSourceName(String jtaDatasource);

    String getJtaDataSourceName();

    void setNonJtaDataSourceName(String nonJtaDatasource);

    String getNonJtaDataSourceName();

    void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl);

    void setManagedClassNames(List<String> classes);

    /** Complete inventories default to the provider-scanned metadata used before Persistence 4. */
    default List<String> getAllClassNames() {
        return getManagedClassNames();
    }

    default void setAllClassNames(List<String> classes) {
        setManagedClassNames(classes);
    }

    default List<String> getManagedPackageDescriptors() {
        return java.util.Collections.emptyList();
    }

    default List<String> getManagedModuleDescriptors() {
        return java.util.Collections.emptyList();
    }

    default List<String> getAllPackageDescriptors() {
        return getManagedPackageDescriptors();
    }

    default List<String> getAllModuleDescriptors() {
        return getManagedModuleDescriptors();
    }

    default void setManagedPackageDescriptors(List<String> packages) { }
    default void setManagedModuleDescriptors(List<String> modules) { }
    default void setAllPackageDescriptors(List<String> packages) { }
    default void setAllModuleDescriptors(List<String> modules) { }
    default void setDefaultToOneFetchType(jakarta.persistence.FetchType fetchType) { }

    default Object createContainerManagedEntityAgent(jakarta.persistence.EntityManagerFactory factory, java.util.Map<String, String> properties) {
        throw new UnsupportedOperationException("Persistence agents require Jakarta Persistence 4");
    }

    void setExcludeUnlistedClasses(boolean excludeUnlistedClasses);

    /** Container transaction types, converted to the provider's enum by its metadata implementation. */
    enum TransactionType {
        JTA, RESOURCE_LOCAL
    }

    void setTransactionType(TransactionType transactionType);

    /**
     * Transaction type independent of the Jakarta Persistence API version. Shared subsystem code
     * uses this accessor because PersistenceUnitInfo.getTransactionType() changes enum packages
     * between Jakarta Persistence 3.x and 4.0.
     *
     * @return the transaction type, or {@code null} if unspecified
     */
    TransactionType getPersistenceUnitTransactionType();

    void setMappingFiles(List<String> mappingFiles);

    void setJarFileUrls(List<URL> jarFilesUrls);

    List<String> getJarFiles();

    void setJarFiles(List<String> jarFiles);

    void setValidationMode(ValidationMode validationMode);

    void setProperties(Properties props);

    void setPersistenceXMLSchemaVersion(String version);

    void setClassLoader(ClassLoader cl);

    void setTempClassLoaderFactory(TempClassLoaderFactory tempClassLoaderFactory);

    /**
     * Cache a (new, on first use) temp classloader and return it for all subsequent calls.
     * The cached temp classloader is only to be reused by the caller, at the per persistence unit level.
     * @return the cached temp classloader
     */
    ClassLoader cacheTempClassLoader();

    void setSharedCacheMode(SharedCacheMode sharedCacheMode);

    List<ClassTransformer> getTransformers();

    boolean needsJPADelegatingClassFileTransformer();

    void setScopeAnnotationName(String element);
    public String getScopeAnnotationName();



    void setQualifierAnnotationNames(List<String> qualifiers);
    List<String> getQualifierAnnotationNames();

    boolean isDuplicate();

    void setDuplicate(boolean b);
}
