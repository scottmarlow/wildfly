/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.extension.mod_cluster;

import static org.jboss.as.clustering.dmr.ModelNodes.optionalString;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SECURITY_KEY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SOCKET;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.AUTO_ENABLE_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.BALANCER;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.EXCLUDED_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_PACKETS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_WAIT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.LOAD_BALANCING_GROUP;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.MAX_ATTEMPTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.NODE_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PING;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PROXIES;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PROXY_URL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SESSION_DRAINING_STRATEGY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SMAX;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SOCKET_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SSL_CONTEXT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STOP_CONTEXT_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.TTL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.WORKER_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_CERTIFICATE_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_REVOCATION_URL;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CERTIFICATE_KEY_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PASSWORD;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PROTOCOL;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.builder.ModClusterConfigurationBuilder;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.mcmp.impl.JSSESocketFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class ModClusterConfigurationServiceConfigurator implements ResourceServiceConfigurator, Supplier<ModClusterConfiguration> {

    private final ModClusterConfigurationBuilder builder = new ModClusterConfigurationBuilder();
    private final List<SupplierDependency<OutboundSocketBinding>> outboundSocketBindings = new LinkedList<>();

    private volatile SupplierDependency<SocketBinding> advertiseSocketDependency = null;
    private volatile SupplierDependency<SSLContext> sslContextDependency = null;

    @Override
    public ServiceName getServiceName() {
        return ContainerEventHandlerServiceConfigurator.CONFIG_SERVICE_NAME;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {

        // Advertise
        optionalString(ADVERTISE_SOCKET.resolveModelAttribute(context, model))
                .ifPresent(advertiseSocketRef -> this.advertiseSocketDependency = new ServiceSupplierDependency<>(context.getCapabilityServiceName(CommonUnaryRequirement.SOCKET_BINDING.getName(), advertiseSocketRef, SocketBinding.class)));
        optionalString(ADVERTISE_SECURITY_KEY.resolveModelAttribute(context, model))
                .ifPresent(securityKey -> builder.advertise().setAdvertiseSecurityKey(securityKey));

        // MCMP

        builder.mcmp()
                .setAdvertise(ADVERTISE.resolveModelAttribute(context, model).asBoolean())
                .setProxyURL(PROXY_URL.resolveModelAttribute(context, model).asString())
                .setAutoEnableContexts(AUTO_ENABLE_CONTEXTS.resolveModelAttribute(context, model).asBoolean())
                .setStopContextTimeout(STOP_CONTEXT_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setStopContextTimeoutUnit(TimeUnit.valueOf(STOP_CONTEXT_TIMEOUT.getMeasurementUnit().getName()))
                .setSocketTimeout(SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt() * 1000)
                .setSessionDrainingStrategy(Enum.valueOf(SessionDrainingStrategyEnum.class, SESSION_DRAINING_STRATEGY.resolveModelAttribute(context, model).asString()))
        ;

        if (model.hasDefined(CommonAttributes.EXCLUDED_CONTEXTS)) {
            String contexts = EXCLUDED_CONTEXTS.resolveModelAttribute(context, model).asString();
            Map<String, Set<String>> excludedContextsPerHost;
            if (contexts == null) {
                excludedContextsPerHost = Collections.emptyMap();
            } else {
                String trimmedContexts = contexts.trim();

                if (trimmedContexts.isEmpty()) {
                    excludedContextsPerHost = Collections.emptyMap();
                } else {
                    excludedContextsPerHost = new HashMap<>();

                    for (String c : trimmedContexts.split(",")) {
                        String[] parts = c.trim().split(":");

                        if (parts.length > 2) {
                            throw ROOT_LOGGER.excludedContextsWrongFormat(trimmedContexts);
                        }

                        String host = null;
                        String trimmedContext = parts[0].trim();

                        if (parts.length == 2) {
                            host = trimmedContext;
                            trimmedContext = parts[1].trim();
                        }

                        String path;
                        switch (trimmedContext) {
                            case "ROOT":
                                ROOT_LOGGER.excludedContextsUseSlashInsteadROOT();
                            case "/":
                                path = "";
                                break;
                            default:
                                // normalize the context by pre-pending or removing trailing slash
                                trimmedContext = trimmedContext.startsWith("/") ? trimmedContext : ("/" + trimmedContext);
                                path = trimmedContext.endsWith("/") ? trimmedContext.substring(0, trimmedContext.length() - 1) : trimmedContext;
                                break;
                        }

                        Set<String> paths = excludedContextsPerHost.computeIfAbsent(host, k -> new HashSet<>());

                        paths.add(path);
                    }
                }
            }
            builder.mcmp().setExcludedContextsPerHost(excludedContextsPerHost);
        }

        // Balancer

        builder.balancer()
                .setStickySession(STICKY_SESSION.resolveModelAttribute(context, model).asBoolean())
                .setStickySessionRemove(STICKY_SESSION_REMOVE.resolveModelAttribute(context, model).asBoolean())
                .setStickySessionForce(STICKY_SESSION_FORCE.resolveModelAttribute(context, model).asBoolean())
                .setWorkerTimeout(WORKER_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setMaxAttempts(MAX_ATTEMPTS.resolveModelAttribute(context, model).asInt())
        ;

        // Node

        builder.node()
                .setFlushPackets(FLUSH_PACKETS.resolveModelAttribute(context, model).asBoolean())
                .setFlushWait(FLUSH_WAIT.resolveModelAttribute(context, model).asInt())
                .setPing(PING.resolveModelAttribute(context, model).asInt())
                .setSmax(SMAX.resolveModelAttribute(context, model).asInt())
                .setTtl(TTL.resolveModelAttribute(context, model).asInt())
                .setNodeTimeout(NODE_TIMEOUT.resolveModelAttribute(context, model).asInt())
        ;
        ModelNode node = BALANCER.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setBalancer(node.asString());
        }
        node = LOAD_BALANCING_GROUP.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setLoadBalancingGroup(node.asString());
        }
        node = PROXIES.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            for (ModelNode ref : node.asList()) {
                String asString = ref.asString();
                this.outboundSocketBindings.add(new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, asString)));
            }
        }

        if (model.hasDefined(CommonAttributes.PROXY_LIST)) {
            throw new OperationFailedException(ROOT_LOGGER.proxyListNotAllowedInCurrentModel());
        }


        // Elytron-based security support

        node = SSL_CONTEXT.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            this.sslContextDependency = new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, node.asString()));
        }

        // Legacy security support

        if (model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            if (node.isDefined()) {
                throw ROOT_LOGGER.bothElytronAndLegacySslContextDefined();
            }
            ModelNode sslModel = model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair());

            ModClusterConfig sslConfiguration = new ModClusterConfig();

            node = KEY_ALIAS.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslKeyAlias(node.asString());
            }
            node = PASSWORD.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                String str = node.toString();
                sslConfiguration.setSslTrustStorePassword(str);
                sslConfiguration.setSslKeyStorePassword(str);
            }
            node = CERTIFICATE_KEY_FILE.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslKeyStore(node.asString());
            }
            node = CIPHER_SUITE.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslCiphers(node.asString());
            }
            node = PROTOCOL.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslProtocol(node.asString());
            }
            node = CA_CERTIFICATE_FILE.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslTrustStore(node.asString());
            }
            node = CA_REVOCATION_URL.resolveModelAttribute(context, sslModel);
            if (node.isDefined()) {
                sslConfiguration.setSslCrlFile(node.asString());
            }

            builder.mcmp().setSocketFactory(new JSSESocketFactory(sslConfiguration));
        }

        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ModClusterConfiguration> config = new CompositeDependency(this.advertiseSocketDependency, this.sslContextDependency).register(builder).provides(this.getServiceName());
        for (Dependency dependency : this.outboundSocketBindings) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(config, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public ModClusterConfiguration get() {

        // Advertise
        if (advertiseSocketDependency != null) {
            final SocketBinding binding = advertiseSocketDependency.get();
            builder.advertise()
                    .setAdvertiseSocketAddress(binding.getMulticastSocketAddress())
                    .setAdvertiseInterface(binding.getNetworkInterfaceBinding().getAddress())
            ;
            if (!isMulticastEnabled(binding.getSocketBindings().getDefaultInterfaceBinding().getNetworkInterfaces())) {
                ROOT_LOGGER.multicastInterfaceNotAvailable();
            }
        }

        // Proxies
        List<ProxyConfiguration> proxies = new LinkedList<>();
        for (final Supplier<OutboundSocketBinding> outboundSocketBindingValueDependency : outboundSocketBindings) {
            OutboundSocketBinding binding = outboundSocketBindingValueDependency.get();
            proxies.add(new ProxyConfiguration() {

                @Override
                public InetSocketAddress getRemoteAddress() {
                    // Both host and port may not be null in the model, no need to validate here
                    // Don't do resolving here, let mod_cluster deal with it
                    return new InetSocketAddress(binding.getUnresolvedDestinationAddress(), binding.getDestinationPort());
                }

                @Override
                public InetSocketAddress getLocalAddress() {
                    if (binding.getOptionalSourceAddress() != null) {
                        return new InetSocketAddress(binding.getOptionalSourceAddress(), binding.getAbsoluteSourcePort() == null ? 0 : binding.getAbsoluteSourcePort());
                    } else if (binding.getAbsoluteSourcePort() != null) {
                        // Bind to port only if source address is not configured
                        return new InetSocketAddress(binding.getAbsoluteSourcePort());
                    }
                    // No binding configured so don't bind
                    return null;
                }

            });
        }
        builder.mcmp().setProxyConfigurations(proxies);

        // SSL
        if (sslContextDependency != null) {
            builder.mcmp().setSocketFactory(sslContextDependency.get().getSocketFactory());
        }

        return builder.build();
    }

    private static boolean isMulticastEnabled(Collection<NetworkInterface> interfaces) {
        for (NetworkInterface iface : interfaces) {
            try {
                if (iface.isUp() && (iface.supportsMulticast() || iface.isLoopback())) {
                    return true;
                }
            } catch (SocketException e) {
                // Ignore
            }
        }
        return false;
    }

}
