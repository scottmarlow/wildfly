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


import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jipijapa.spi.statistics.Statistics;
import org.jipijapa.spi.statistics.StatisticsPlugin;

/**
 * ManagementAccess
 *
 * The persistence provider and jipijapa adapters will be in the same classloader,
 * either a static module or included directly in the application.  Those are the two supported use
 * cases for management of deployment persistence units also.
 *
 * From a management point of view, the requirements are:
 *   1.  show management statistics for static persistence provider modules and applications that have
 *       their own persistence provider module.
 *
 *   2.  persistence provider adapters will provide a unique key that identifies the management version of supported
 *       management statistics/operations.  For example, Hibernate 3.x might be 1.0, Hibernate 4.1/4.2 might
 *       be version 2.0 and Hibernate 4.3 could be 2.0 also as long as its compatible (same stats) with 4.1/4.2.
 *       Eventually, a Hibernate (later version) change in statistics is likely to happen, the management version
 *       will be incremented.
 *
 * @author Scott Marlow
 */
public class ManagementAccess {

    private static final Map<String,Resource> existingManagementStatisticsResource = new HashMap<String, Resource>();
    private static volatile ManagementResourceRegistration jpaSubsystemDeployments;

    public static void setManagementResourceRegistration(ManagementResourceRegistration managementResourceRegistration) {
        jpaSubsystemDeployments = managementResourceRegistration;
    }

    /**
     * Create single instance of management statistics resource per managementAdaptor version.
     *
     * @param managementAdaptor
     * @param scopedPersistenceUnitName
     * @return
     */
    public static Resource createManagementStatisticsResource(final ManagementAdaptor managementAdaptor, final String scopedPersistenceUnitName) {
        synchronized (existingManagementStatisticsResource) {
            final EntityManagerFactoryLookup entityManagerFactoryLookup = new EntityManagerFactoryLookup(scopedPersistenceUnitName);
            Resource result = existingManagementStatisticsResource.get(managementAdaptor.getVersion());
            if (result == null) {

                final StatisticsPlugin statisticsPlugin = managementAdaptor.getStatisticsPlugin();
                final Statistics statistics = statisticsPlugin.getStatistics();
                // setup statistics
                ResourceDescriptionResolver resourceDescriptionResolver = new StandardResourceDescriptionResolver(
                        statistics.getResourceBundleKeyPrefix(), statistics.getResourceBundleName(), statistics.getClass().getClassLoader());

                jpaSubsystemDeployments.registerSubModel(
                        new ManagementResourceDefinition(PathElement.pathElement(managementAdaptor.getIdentificationLabel()), resourceDescriptionResolver, statistics, entityManagerFactoryLookup));

                result = new ManagementStatisticsResource(statistics, scopedPersistenceUnitName, managementAdaptor.getIdentificationLabel());

                existingManagementStatisticsResource.put(managementAdaptor.getVersion(),result);

            }
            return result;
        }
    }

    private static class ManagementResourceDefinition extends SimpleResourceDefinition {

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

        // TODO: also do subsystem.get(CHILDREN, "entity-cache", MODEL_DESCRIPTION); // placeholder
        // subsystem.get(CHILDREN, "entity-cache", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE));
        // either roll the following loop into ManagementResourceDefinition (would only handle single level of nesting) or not

        for( final String sublevelChildName : statistics.getChildrenNames()) {
            Statistics sublevelStatistics = statistics.getChildren(sublevelChildName);
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
                                /** TODO: npe for statistics==org.jboss.as.jpa.hibernate4.management.HibernateEntityCacheStatistics, statisticName==second-level-cache-count-in-memory
                                 * {    "address" => [
                                         ("deployment" => "2lc.jar"),
                                         ("subsystem" => "jpa"),
                                         ("hibernate-persistence-unit" => "2lc.jar#TEST_PU"),
                                         ("entity-cache" => "entity-cache-region-name")
                                     ],
                                     "operation" => "read-attribute",
                                     "name" => "second-level-cache-count-in-memory",
                                     "operation-headers" => {"caller-type" => "user"}
                                 }
                                 */

                                Object result = statistics.getValue(statisticName, entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
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
                                    Object oldSetting = statistics.getValue(statisticName, entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
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

                                    Object result = statistics.getValue(statisticName, entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
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
                            Object result = statistics.getValue(statisticName, entityManagerFactoryLookup, StatisticNameLookup.statisticNameLookup(statisticName));
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
