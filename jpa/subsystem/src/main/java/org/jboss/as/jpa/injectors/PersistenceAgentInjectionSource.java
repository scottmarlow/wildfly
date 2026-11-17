/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.injectors;

import java.util.Map;
import java.util.HashMap;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/** Binding for an EE container-managed persistence agent. */
public final class PersistenceAgentInjectionSource extends InjectionSource {
    private final ServiceName serviceName;
    private final ServiceRegistry registry;
    private final PersistenceUnitMetadata metadata;
    private final Map<String, String> properties;

    public PersistenceAgentInjectionSource(ServiceName serviceName, ServiceRegistry registry,
                                           PersistenceUnitMetadata metadata, Map<String, String> properties) {
        this.serviceName = serviceName;
        this.registry = registry;
        this.metadata = metadata;
        this.properties = new HashMap<>(properties);
    }

    @Override
    public void getResourceValue(ResolutionContext context, ServiceBuilder<?> builder,
                                 DeploymentPhaseContext phase, Injector<ManagedReferenceFactory> injector) {
        builder.requires(serviceName);
        injector.inject(() -> {
            PersistenceUnitServiceImpl service = (PersistenceUnitServiceImpl) registry.getRequiredService(serviceName).getValue();
            return new ValueManagedReference(metadata.createContainerManagedEntityAgent(service.getEntityManagerFactory(), properties));
        });
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PersistenceAgentInjectionSource)) {
            return false;
        }
        PersistenceAgentInjectionSource source = (PersistenceAgentInjectionSource) other;
        return serviceName.equals(source.serviceName) && properties.equals(source.properties);
    }

    @Override
    public int hashCode() {
        return 31 * serviceName.hashCode() + properties.hashCode();
    }
}
