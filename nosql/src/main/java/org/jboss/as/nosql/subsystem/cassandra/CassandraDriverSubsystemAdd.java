/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.nosql.subsystem.cassandra;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.nosql.driver.cassandra.CassandraDriverService;
import org.jboss.as.nosql.driver.cassandra.ConfigurationBuilder;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

/**
 * CassandraDriverSubsystemAdd
 *
 * @author Scott Marlow
 */
public class CassandraDriverSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final CassandraDriverSubsystemAdd INSTANCE = new CassandraDriverSubsystemAdd();
    private static final ServiceName CASSANDRADBSERVICE = ServiceName.JBOSS.append("cassandradriver");
    private final ParametersValidator runtimeValidator = new ParametersValidator();

    private CassandraDriverSubsystemAdd() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : CassandraDriverDefinition.INSTANCE.getAttributes()) {
            def.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws
            OperationFailedException {

        runtimeValidator.validate(operation.resolve());
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(CassandraDriverExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_PERSISTENCE_UNIT+1, new CassandraDriverScanDependencyProcessor());
                processorTarget.addDeploymentProcessor(CassandraDriverExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_PERSISTENCE_ANNOTATION+1, new CassandraDriverDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final ModelNode cassandraSubsystem = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        if( cassandraSubsystem.hasDefined(CommonAttributes.PROFILE)) {
            Map<String, String> jndiNameToModuleName = new HashMap<>();
            for( ModelNode profiles : cassandraSubsystem.get(CommonAttributes.PROFILE).asList()) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                for(ModelNode profileEntry : profiles.get(0).asList()) {
                    if(profileEntry.hasDefined(CommonAttributes.ID_NAME)) {
                        builder.setDescription(profileEntry.get(CommonAttributes.ID_NAME).asString());
                    }
                    else if(profileEntry.hasDefined(CommonAttributes.JNDI_NAME)) {
                        builder.setJNDIName(profileEntry.get(CommonAttributes.JNDI_NAME).asString());
                    }
                    else if(profileEntry.hasDefined(CommonAttributes.MODULE_NAME)) {
                        builder.setModuleName(profileEntry.get(CommonAttributes.MODULE_NAME).asString());
                    }
                    else if(profileEntry.hasDefined(CommonAttributes.DATABASE)) {
                        builder.setKeyspace(profileEntry.get(CommonAttributes.DATABASE).asString());
                    }
                    else if(profileEntry.hasDefined(CommonAttributes.HOST_DEF)) {
                        ModelNode hostModels = profileEntry.get(CommonAttributes.HOST_DEF);
                        for(ModelNode host: hostModels.asList()) {
                            String hostname=null;
                            int port=0;
                            for(ModelNode hostEntry: host.get(0).asList()) {
                                if(hostEntry.hasDefined(CommonAttributes.HOST)) {
                                    hostname = hostEntry.get(CommonAttributes.HOST).asString();
                                }
                                else if(hostEntry.hasDefined(CommonAttributes.PORT)) {
                                    port = hostEntry.get(CommonAttributes.PORT).asInt();
                                }
                            }
                            if(port > 0) {
                                builder.addTarget(hostname, port);
                            } else {
                                builder.addTarget(hostname);
                            }
                        }
                    }
                }
                startCassandraDriverService(context, builder,jndiNameToModuleName);
            }
            startCassandraDriverSubsysteService(context, jndiNameToModuleName);
        }
    }

    private void startCassandraDriverSubsysteService(final OperationContext context, final Map<String, String> jndiNameToModuleName) {
        CassandraSubsystemService cassandraSubsystemService = new CassandraSubsystemService(jndiNameToModuleName);
        context.getServiceTarget().addService(CassandraSubsystemService.serviceName(), cassandraSubsystemService).setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private void startCassandraDriverService(OperationContext context, ConfigurationBuilder builder, Map<String, String> jndiNameToModuleName) {
        if(builder.getJNDIName() !=null && builder.getJNDIName().length() > 0) {
            final CassandraDriverService service = new CassandraDriverService(builder);
            final ServiceName serviceName = CASSANDRADBSERVICE.append( builder.getDescription());
            final ContextNames.BindInfo bindingInfo = ContextNames.bindInfoFor(builder.getJNDIName());

            if(builder.getModuleName() != null) {
                // maintain a mapping from JNDI name to NoSQL module name, that we will use during deployment time to
                // identify the static module name to add to the deployment.
                jndiNameToModuleName.put(builder.getJNDIName(), builder.getModuleName());
            }

            final BinderService binderService = new BinderService(bindingInfo.getBindName());
            context.getServiceTarget().addService(bindingInfo.getBinderServiceName(), binderService)
                .addDependency(CassandraSubsystemService.serviceName())
                .addDependency(bindingInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(serviceName, CassandraDriverService.class, new Injector<CassandraDriverService>() {
                    @Override
                    public void inject(final CassandraDriverService value) throws
                            InjectionException {
                        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<>(value.getSession() != null ? value.getSession() : value.getCluster())));
                    }

                    @Override
                    public void uninject() {
                        binderService.getNamingStoreInjector().uninject();
                    }
                }).install();
            context.getServiceTarget().addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
    }

}
