package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

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
 *
 *
 * @author Scott Marlow
 */
public class PersistenceIntegrationWithCDI implements Extension {

    public PersistenceIntegrationWithCDI(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager manager) {

    }



}
