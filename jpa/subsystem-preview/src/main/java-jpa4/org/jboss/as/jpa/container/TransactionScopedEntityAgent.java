/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.io.Serializable;
import java.util.function.Function;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManagerFactory;

import org.jipijapa.plugin.spi.ScopedStatelessSessionInvocationHandler;
import org.jipijapa.plugin.spi.ScopedStatelessSessionSupplier;

/**
 * Creates a container-managed, transaction-scoped Jakarta Persistence 4 {@link EntityAgent} for injection via
 * {@code @PersistenceAgent} (or a CDI injection point).
 * <p>
 * The returned {@link EntityAgent} is a proxy that, per invocation, delegates to a transaction-scoped underlying
 * {@code EntityAgent} obtained from {@link EntityManagerFactory#createEntityAgent()}. Scoping and cleanup reuse
 * the same transaction / non-transaction machinery as the stateless-session support: within a Jakarta
 * Transactions transaction the agent is created once and closed on transaction completion; outside a transaction
 * it is created per invocation and closed by the {@code NonTxEmCloser} when the component invocation completes.
 * Like a transaction-scoped {@code EntityManager}, {@code close()} on the injected proxy is a no-op.
 *
 * @author Scott Marlow
 */
public final class TransactionScopedEntityAgent {

    private TransactionScopedEntityAgent() {
    }

    /**
     * Creates a transaction-scoped {@link EntityAgent} proxy for the given persistence unit.
     *
     * @param puScopedName the fully scoped persistence unit name
     * @param emf          the persistence unit's entity manager factory
     * @return a transaction-scoped {@link EntityAgent} proxy
     */
    public static EntityAgent create(String puScopedName, EntityManagerFactory emf) {
        final ScopedStatelessSessionSupplier supplier =
                new StatelessSessionSupplierImpl(emf, new EntityAgentSupplier(), puScopedName);
        return ScopedStatelessSessionInvocationHandler.createStatelessSessionProxy(
                EntityAgent.class, EntityAgent.class.getClassLoader(), supplier);
    }

    /**
     * Serializable factory function used by {@link StatelessSessionSupplierImpl} to create the underlying
     * {@link EntityAgent} from the entity manager factory. Kept as a named class (rather than a lambda) because
     * the enclosing supplier is {@link Serializable}.
     */
    private static final class EntityAgentSupplier implements Function<EntityManagerFactory, AutoCloseable>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public AutoCloseable apply(EntityManagerFactory entityManagerFactory) {
            return entityManagerFactory.createEntityAgent();
        }
    }
}
