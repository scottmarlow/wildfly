package org.jboss.as.jpa.beanmanager;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * PersistenceIntegrationWithCDI will setup Persistence/CDI integration as mentioned in jakarta.ee/specifications/platform/11/jakarta-platform-spec-11.0#a441
 *
 * Obtaining an Entity Manager using CDI:
 *   A Jakarta EE container must feature built-integration of Jakarta Persistence with the CDI bean manager, allowing injection of a container-managed
 *   entity manager factory using the annotation jakarta.inject.Inject.
 *
 *   For each persistence unit, the container must make available a bean with:
 *    - bean type EntityManager,
 *    - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 *    - the scope specified by the scope XML element in persistence.xml, or jakarta.transaction.TransactionScoped if no scope is explicitly specified,
 *    - no interceptor bindings,
 *    - a bean implementation which satisfies the requirements of this specification for a container-managed entity manager.
 *
 *    "EntityManager must also be available via the additional access methods specified at the beginning of this section."
 *
 * Injecting an Entity Manager Factory using CDI:
 *   A Jakarta EE container must feature built-in integration of Jakarta Persistence with the CDI bean manager,
 *   allowing injection of a container-managed entity manager using the annotation jakarta.inject.Inject.
 *
 *   For each persistence unit, the container must make available a bean with:
 *    - bean type EntityManagerFactory,
 *    - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 *    - scope jakarta.enterprise.context.ApplicationScoped,
 *    - bean name given by the name of the persistence unit,
 *    - no interceptor bindings,
 *    - a bean implementation which satisfies the requirements of the Persistence specification for a container-managed entity manager factory.
 *
 * Furthermore, the container must make available five beans with:
 *    - bean types CriteriaBuilder, PersistenceUnitUtil, Cache, SchemaManager, and Metamodel, respectively,
 *    - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 *    - scope jakarta.enterprise.context.Dependent,
 *    - no interceptor bindings,
 *    - a bean implementation which simply obtains the instance of the bean type by calling the appropriate getter method of the EntityManagerFactory bean.
 *    - To access these bean types (CriteriaBuilder, PersistenceUnitUtil, Cache, SchemaManager, and Metamodel) from callsites that use @Resource or JNDI lookup,
 *      users must first obtain the EntityManagerFactory and then use the appropriate getter methods.
 *
 * @author Scott Marlow
 */
public class PersistenceIntegrationWithCDI {

    private static final List<String> defaultQualifier = new ArrayList<String>();
    private static final String transactionScoped = "jakarta.transaction.TransactionScoped";
    private static final String applicationScoped = "jakarta.enterprise.context.ApplicationScoped";
    private static final String dependentScoped = "jakarta.enterprise.context.Dependent";

    static  {
        defaultQualifier.add("jakarta.enterprise.inject.Default");
    }


    public static void addBeans(AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager, PersistenceUnitMetadata persistenceUnitMetadata) {

        // determine the qualifiers to use for creating each bean
        List<String> qualifiers;
        if ( persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0 ) {
            qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
        } else {
            qualifiers = defaultQualifier;
        }

        entityManager(afterBeanDiscovery, beanManager, persistenceUnitMetadata, qualifiers);

        AnnotatedType<EntityManagerFactory> entityManagerFactoryAnnotatedType = beanManager.createAnnotatedType(EntityManagerFactory.class);
        BeanAttributes<EntityManagerFactory> entityManagerFactoryBeanAttributes = beanManager.createBeanAttributes(entityManagerFactoryAnnotatedType);

    }

    private static void entityManager(AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager, PersistenceUnitMetadata persistenceUnitMetadata, List<String> qualifiers) {
        AnnotatedType<EntityManager> entityManagerAnnotatedType = beanManager.createAnnotatedType(EntityManager.class);
        BeanAttributes<EntityManager> entityManagerBeanAttributes = beanManager.createBeanAttributes(entityManagerAnnotatedType);

    }


}
