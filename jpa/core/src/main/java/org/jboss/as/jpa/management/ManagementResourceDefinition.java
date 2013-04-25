/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jipijapa.spi.statistics.Statistics;

/**
* ManagementResourceDefinition
*
* @author Scott Marlow
*/
class ManagementResourceDefinition extends SimpleResourceDefinition {

    private final Statistics statistics;
    private final EntityManagerFactoryLookup entityManagerFactoryLookup;
    private final ResourceDescriptionResolver descriptionResolver;
    private final PathElement pathElement;

    public ManagementResourceDefinition(
            final PathElement pathElement,
            final ResourceDescriptionResolver descriptionResolver,
            final Statistics statistics,
            final EntityManagerFactoryLookup entityManagerFactoryLookup) {
        super(pathElement, descriptionResolver);
        this.pathElement = pathElement;
        this.statistics = statistics;
        this.entityManagerFactoryLookup = entityManagerFactoryLookup;
        this.descriptionResolver = descriptionResolver;
    }

    private ModelType getModelType(Class type) {

        if(Integer.class.equals(type)) {
            return ModelType.INT;
        }
        else if(Long.class.equals(type)) {
            return ModelType.LONG;
        }
        else if(String.class.equals(type)) {
            return ModelType.STRING;
        }
        else if(Boolean.class.equals(type)) {
            return ModelType.BOOLEAN;
        }
        return ModelType.OBJECT;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        for( final String sublevelChildName : statistics.getChildrenNames()) {
            Statistics sublevelStatistics = statistics.getChild(sublevelChildName);
            ResourceDescriptionResolver sublevelResourceDescriptionResolver = new StandardResourceDescriptionResolver(
                    sublevelChildName, sublevelStatistics.getResourceBundleName(), sublevelStatistics.getClass().getClassLoader());
            resourceRegistration.registerSubModel(
                    new ManagementResourceDefinition(PathElement.pathElement(sublevelChildName), sublevelResourceDescriptionResolver, sublevelStatistics, entityManagerFactoryLookup));

        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for(final String statisticName: statistics.getNames()) {
            AttributeDefinition attributeDefinition =
                    new SimpleAttributeDefinitionBuilder(statisticName, getModelType(statistics.getType(statisticName)), true)
                            .setXmlName(statisticName)
                            .setAllowExpression(true)
                            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
                            .build();

            if (statistics.isAttribute(statisticName)) {
                OperationStepHandler readHandler =
                    new AbstractMetricsHandler() {
                        @Override
                        void handle(final ModelNode response, OperationContext context, final ModelNode operation) {
                            final String dynamicName = PathAddress.pathAddress(operation.get(ADDRESS)).getLastElement().getValue();
                            Object result = statistics.getValue(
                                    statisticName,
                                    entityManagerFactoryLookup,
                                    StatisticNameLookup.statisticNameLookup(statisticName),
                                    DynamicNameLookup.dynamicNameLookup(dynamicName));
                            if (result != null) {
                                response.set(result.toString());
                            }
                        }
                    };

                // handle writeable attributes
                if (statistics.isWriteable(statisticName)) {
                    OperationStepHandler writeHandler =
                        new AbstractMetricsHandler() {
                            @Override
                            void handle(final ModelNode response, OperationContext context, final ModelNode operation) {
                                final String dynamicName = PathAddress.pathAddress(operation.get(ADDRESS)).getLastElement().getValue();
                                Object oldSetting = statistics.getValue(
                                        statisticName,
                                        entityManagerFactoryLookup,
                                        StatisticNameLookup.statisticNameLookup(statisticName),
                                        DynamicNameLookup.dynamicNameLookup(dynamicName));
                                {
                                    final ModelNode value = operation.get(ModelDescriptionConstants.VALUE).resolve();

                                    if (Boolean.class.equals(statistics.getType(statisticName))) {
                                        statistics.setValue(statisticName, value.asBoolean(), entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
                                    } else if(Integer.class.equals(statistics.getType(statisticName))) {
                                        statistics.setValue(statisticName, value.asInt(), entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
                                    } else if(Long.class.equals(statistics.getType(statisticName))) {
                                        statistics.setValue(statisticName, value.asLong(), entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
                                    } else {
                                        statistics.setValue(statisticName, value.asString(), entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
                                    }

                                }
                                final Object rollBackValue = oldSetting;
                                context.completeStep(new OperationContext.RollbackHandler() {
                                    @Override
                                    public void handleRollback(OperationContext context, ModelNode operation) {
                                        statistics.setValue(statisticName,rollBackValue, entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
                                    }
                                });

                                Object result = statistics.getValue(
                                        statisticName, entityManagerFactoryLookup,
                                        StatisticNameLookup.statisticNameLookup(statisticName),
                                        DynamicNameLookup.dynamicNameLookup(dynamicName));
                                if (result != null) {
                                    response.set(result.toString());
                                }

                            }
                        };
                    resourceRegistration.registerReadWriteAttribute(attributeDefinition, readHandler, writeHandler);

                }
                else {
                    resourceRegistration.registerReadOnlyAttribute(attributeDefinition, readHandler);
                }
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        for(final String statisticName: statistics.getNames()) {
            if(statistics.isOperation(statisticName)) {
                AttributeDefinition attributeDefinition =
                        new SimpleAttributeDefinitionBuilder(statisticName, getModelType(statistics.getType(statisticName)), true)
                                .setXmlName(statisticName)
                                .setAllowExpression(true)
                                .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
                                .build();

                OperationStepHandler operationHandler =
                new AbstractMetricsHandler() {
                    @Override
                    void handle(final ModelNode response, OperationContext context, final ModelNode operation) {
                        final String dynamicName = PathAddress.pathAddress(operation.get(ADDRESS)).getLastElement().getValue();
                        Object result = statistics.getValue(
                                statisticName, entityManagerFactoryLookup,
                                StatisticNameLookup.statisticNameLookup(statisticName),
                                DynamicNameLookup.dynamicNameLookup(dynamicName));
                        if (result != null) {
                            response.set(result.toString());
                        }
                    }
                };

                SimpleOperationDefinition definition =
                    new SimpleOperationDefinition(statisticName, descriptionResolver, attributeDefinition);
                resourceRegistration.registerOperationHandler(definition, operationHandler);
            }
        }
    }

    private abstract static class AbstractMetricsHandler extends AbstractRuntimeOnlyHandler {

        abstract void handle(final ModelNode response, final OperationContext context, final ModelNode operation);

        @Override
        protected void executeRuntimeStep(final OperationContext context, final ModelNode operation) throws
                OperationFailedException {
            handle(context.getResult(), context, operation);
            context.stepCompleted();
        }
    }

}
