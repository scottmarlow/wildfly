package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
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


    public static void addBeans(BeanManager beanManager, EntityManagerFactory entityManagerFactory, PersistenceUnitMetadata persistenceUnitMetadata, ClassLoader classLoader) {

        // determine the qualifiers to use for creating each bean
        List<String> qualifiers;
        if ( persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0 ) {
            qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
        } else {
            qualifiers = defaultQualifier;
        }


        entityManager(beanManager, persistenceUnitMetadata, entityManagerFactory,  qualifiers, classLoader);

    }

    private static void entityManager(
            BeanManager beanManager,
            PersistenceUnitMetadata persistenceUnitMetadata,
            EntityManagerFactory entityManagerFactory,
            List<String> qualifiers, ClassLoader classLoader) {
        String scope = persistenceUnitMetadata.getScopeAnnotationName() != null? persistenceUnitMetadata.getScopeAnnotationName(): transactionScoped;
        AnnotatedType<EntityManager> entityManagerAnnotatedType = beanManager.createAnnotatedType(EntityManager.class);

        BeanAttributes<EntityManager> entityManagerBeanAttributes = beanManager.createBeanAttributes(entityManagerAnnotatedType);
        try {
            entityManagerBeanAttributes.getQualifiers().add(((Class<? extends Annotation>)classLoader.loadClass(scope)).newInstance());
            for (String qualifier : qualifiers) {
                entityManagerBeanAttributes.getQualifiers().add(((Class<? extends Annotation>)classLoader.loadClass(qualifier)).newInstance());
            }
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        beanManager. createBean(entityManagerBeanAttributes, EntityManager.class, new ProducerFactory() {

              @Override
              public <T> Producer<T> createProducer(Bean bean) {
                  return new WrappingProducer<T>();
              }
          });
    }

    EntityManager entityManager(PersistenceUnitMetadata persistenceUnitMetadata) {

    return new TransactionScopedEntityManager(
            persistenceUnitMetadata.getScopeAnnotationName(),
            new Properties(),
            entityManagerFactory,
            SynchronizationType.SYNCHRONIZED,
            TransactionSynchronizationRegistry,
            transactionManager);
}


    private static class WrappingProducer<T> implements Producer<T> {
        public WrappingProducer() {

        }

        @Override
        public T produce(CreationalContext<T> ctx) {
            return null;
        }

        @Override
        public void dispose(T instance) {

        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Set.of();
        }
    }
}
