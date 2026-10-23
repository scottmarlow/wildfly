/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.io.Serializable;
import java.util.function.Function;
import java.util.Map;
import java.util.TreeMap;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManagerFactory;

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
 * Container lifecycle methods are enforced by the injected proxy.
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
        return create(puScopedName, emf, java.util.Collections.emptyMap());
    }

    public static EntityAgent create(String puScopedName, EntityManagerFactory emf, Map<String, String> properties) {
        if (emf.getTransactionType() != jakarta.persistence.PersistenceUnitTransactionType.JTA) {
            throw new IllegalArgumentException("Container-managed EntityAgent requires a JTA persistence unit");
        }
        Map<String, String> configuration = new TreeMap<>(properties);
        StringBuilder key = new StringBuilder("EntityAgent:").append(puScopedName.length()).append(':').append(puScopedName);
        configuration.forEach((name, value) -> key.append(':').append(name.length()).append(':').append(name)
                .append(':').append(value.length()).append(':').append(value));
        final ScopedStatelessSessionSupplier supplier =
                new StatelessSessionSupplierImpl(emf, new EntityAgentSupplier(configuration), puScopedName, key.toString());
        return (EntityAgent) Proxy.newProxyInstance(EntityAgent.class.getClassLoader(),
                new Class<?>[]{EntityAgent.class}, new AgentInvocationHandler(supplier));
    }

    private static final class AgentInvocationHandler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;
        private final ScopedStatelessSessionSupplier supplier;

        AgentInvocationHandler(ScopedStatelessSessionSupplier supplier) {

            this.supplier = supplier;

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "equals": return proxy == args[0];
                    case "hashCode": return System.identityHashCode(proxy);
                    case "toString": return "Container-managed EntityAgent@" + Integer.toHexString(System.identityHashCode(proxy));
                    default: break;
                }
            }
            switch (method.getName()) {
                case "close": throw new IllegalStateException("Cannot close a container-managed EntityAgent");
                case "getTransaction": throw new IllegalStateException("Container-managed EntityAgent uses JTA transactions");
                case "isOpen": return true;
                case "unwrap":
                    if (((Class<?>) args[0]).isInstance(proxy)) {
                        return proxy;
                    }
                    break;
                default: break;
            }
            switch (method.getName()) {
                case "insert":
                case "insertMultiple":
                case "update":
                case "updateMultiple":
                case "delete":
                case "deleteMultiple":
                case "upsert":
                case "upsertMultiple":
                case "refresh":
                case "refreshMultiple":
                    if (!org.jboss.as.jpa.transaction.TransactionUtil.isInTx(
                            org.wildfly.transaction.client.ContextTransactionManager.getInstance())) {
                        throw new jakarta.persistence.TransactionRequiredException("EntityAgent operation requires an active JTA transaction");
                    }
                    break;
                default: break;
            }
            try {
                return method.invoke(supplier.get(), args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    /**
     * Serializable factory function used by {@link StatelessSessionSupplierImpl} to create the underlying
     * {@link EntityAgent} from the entity manager factory. Kept as a named class (rather than a lambda) because
     * the enclosing supplier is {@link Serializable}.
     */
    private static final class EntityAgentSupplier implements Function<EntityManagerFactory, AutoCloseable>, Serializable {

        private static final long serialVersionUID = 1L;

        private final Map<String, String> properties;

        EntityAgentSupplier(Map<String, String> properties) {

            this.properties = properties;

        }

        @Override
        public AutoCloseable apply(EntityManagerFactory entityManagerFactory) {
            return entityManagerFactory.createEntityAgent(properties);
        }
    }
}
