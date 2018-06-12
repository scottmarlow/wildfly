/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectorDependency;
import org.wildfly.clustering.service.ServiceDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * @author Paul Ferraro
 */
public class CredentialSourceDependency implements SupplierDependency<CredentialSource> {

    private final ExceptionSupplier<CredentialSource, Exception> supplier;
    private final Iterable<Dependency> dependencies;

    public CredentialSourceDependency(OperationContext context, Attribute attribute, ModelNode model) throws OperationFailedException {
        DependencyCollectingServiceBuilder builder = new DependencyCollectingServiceBuilder();
        this.supplier = CredentialReference.getCredentialSourceSupplier(context, (ObjectTypeAttributeDefinition) attribute.getDefinition(), model, builder);
        this.dependencies = builder;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public CredentialSource get() {
        try {
            return this.supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static class DependencyCollectingServiceBuilder extends DelegatingServiceBuilder<Object> implements Iterable<Dependency> {
        private final List<Dependency> dependencies = new LinkedList<>();

        DependencyCollectingServiceBuilder() {
            super(null);
        }

        @Override
        protected ServiceBuilder<Object> getDelegate() {
            throw new IllegalStateException();
        }

        @Override
        public Iterator<Dependency> iterator() {
            return this.dependencies.iterator();
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependencies(ServiceName... serviceNames) {
            this.addDependencies(Arrays.asList(serviceNames));
            return this;
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependencies(ServiceBuilder.DependencyType dependencyType, ServiceName... serviceNames) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependencies(serviceNames);
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependencies(Iterable<ServiceName> serviceNames) {
            for (ServiceName serviceName : serviceNames) {
                this.dependencies.add(new ServiceDependency(serviceName));
            }
            return this;
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependencies(ServiceBuilder.DependencyType dependencyType, Iterable<ServiceName> serviceNames) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependencies(serviceNames);
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependency(ServiceName serviceName) {
            this.dependencies.add(new ServiceDependency(serviceName));
            return this;
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependency(serviceName);
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependency(ServiceName serviceName, Injector<Object> target) {
            return this.addDependency(serviceName, Object.class, target);
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName, Injector<Object> target) {
            return this.addDependency(dependencyType, serviceName, Object.class, target);
        }

        @Deprecated
        @Override
        public <I> ServiceBuilder<Object> addDependency(ServiceName serviceName, Class<I> type, Injector<I> target) {
            this.dependencies.add(new InjectorDependency<>(serviceName, type, target));
            return this;
        }

        @Deprecated
        @Override
        public <I> ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName, Class<I> type, Injector<I> target) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependency(serviceName, type, target);
        }
    }
}
