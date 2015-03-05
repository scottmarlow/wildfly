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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.nosql.driver.cassandra.CassandraDriverService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

/**
 * CassandraDefinition
 *
 * @author Scott Marlow
 */
public class CassandraDefinition extends PersistentResourceDefinition {

    public static final ServiceName CASSANDRADRIVERDBSERVICE = ServiceName.JBOSS.append("cassandradriver");

    private static final List<? extends PersistentResourceDefinition> CHILDREN;

    static {
        List<PersistentResourceDefinition>  children = new ArrayList<>();
        children.add(HostsDefinition.INSTANCE);
        CHILDREN = Collections.unmodifiableList(children);
    }

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.PROFILE_NAME, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.JNDI_NAME, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition HOST =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.HOST, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition DATABASE =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DATABASE, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition PORT =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.PORT, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();


    protected static List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            NAME,
            JNDI_NAME,
            HOST,
            PORT,
            DATABASE);

    static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }

    }

    static final CassandraDefinition INSTANCE = new CassandraDefinition();

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    private CassandraDefinition() {
        super(CassandraDriverExtension.PROFILE_PATH,
                CassandraDriverExtension.getResolver(CommonAttributes.PROFILE),
                new ProfileAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    private static class ProfileAdd extends AbstractAddStepHandler {

        private ProfileAdd() {
            super(ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final ModelNode nameModel = NAME.resolveModelAttribute(context, model);
            final ModelNode jndiNameModel = JNDI_NAME.resolveModelAttribute(context, model);
            final ModelNode hostNameModel = HOST.resolveModelAttribute(context, model);
            final ModelNode portNumberModel = PORT.resolveModelAttribute(context, model);
            final ModelNode databaseNameModel = DATABASE.resolveModelAttribute(context, model);

            final String profileName = nameModel.isDefined() ? nameModel.asString() : null;
            final String jndiName = jndiNameModel.isDefined() ? jndiNameModel.asString() : null;
            final String hostName = hostNameModel.isDefined() ? hostNameModel.asString() : null;
            final int port = portNumberModel.isDefined() ? portNumberModel.asInt() : 0;
            final String databaseName = databaseNameModel.isDefined() ? databaseNameModel.asString() : null;

            for( PersistentResourceDefinition resourceDefinition: HostsDefinition.CHILDREN) {
                for (AttributeDefinition attr : resourceDefinition.getAttributes()) {
                    ModelNode value = attr.resolveModelAttribute(context, model);
                    if (!value.isDefined()) {
                        continue;
                    }
                    if (attr.getType() == ModelType.INT) {
                        int p = value.asInt();
                        p = value.asInt();
                    } else if (attr.getType() == ModelType.STRING) {
                        String n = value.asString();
                        n = value.asString();
                    }
                }
            }

            // (CHILDREN.get(0)).getChildren().get(0).getAttributes()
            for (SimpleAttributeDefinition attr : HostDefinition.ATTRIBUTES) {
                ModelNode value = attr.resolveModelAttribute(context, model);
                if (!value.isDefined()) {
                    continue;
                }
                if (attr.getType() == ModelType.INT) {
                    int p = value.asInt();
                    p = value.asInt();
                } else if (attr.getType() == ModelType.STRING) {
                    String n = value.asString();
                    n = value.asString();
                }
            }

            final CassandraDriverService service = new CassandraDriverService(profileName, hostName, port, databaseName);
            final ServiceName serviceName = CASSANDRADRIVERDBSERVICE.append(profileName);
            if(jndiName !=null && jndiName.length() > 0) {
                final ContextNames.BindInfo bindingInfo = ContextNames.bindInfoFor(jndiName);

                final BinderService binderService = new BinderService(bindingInfo.getBindName());
                context.getServiceTarget().addService(bindingInfo.getBinderServiceName(), binderService)
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
            }
            context.getServiceTarget().addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
    }

}
