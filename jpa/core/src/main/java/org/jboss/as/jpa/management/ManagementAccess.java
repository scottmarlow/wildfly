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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
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
 *
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
     * TODO: allow separate resource per managementAdaptor classloader as well for deployments that include their own
     * adaptor.
     *
     * @param managementAdaptor
     * @param scopedPersistenceUnitName
     * @return
     */
    public static Resource createManagementStatisticsResource(final ManagementAdaptor managementAdaptor, final String scopedPersistenceUnitName) {
        synchronized (existingManagementStatisticsResource) {
            Resource result = existingManagementStatisticsResource.get(managementAdaptor.getVersion());
            if (result == null) {
                final StatisticsPlugin statisticsPlugin = managementAdaptor.getStatisticsPlugin();
                final Statistics statistics = statisticsPlugin.getStatistics();
                // setup statistics
                setupStatistics(statistics, jpaSubsystemDeployments,statisticsPlugin.TOPLEVEL_DESCRIPTION_RESOURCEBUNDLE_KEY, managementAdaptor.getIdentificationLabel());
                result = new ManagementStatisticsResource(managementAdaptor, scopedPersistenceUnitName);
                existingManagementStatisticsResource.put(managementAdaptor.getVersion(),result);
            }
            return result;
        }
    }

    private static void setupStatistics(final Statistics statistics, ManagementResourceRegistration managementResourceRegistration, final String descriptionKey, String identificationLabel) {
        DescriptionProvider descriptions = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                // get description/type for each Hibernate statistic
                return describe(statistics, descriptionKey, locale);
            }
        };

        final ManagementResourceRegistration jpaHibernateRegistration =
            managementResourceRegistration.registerSubModel(PathElement.pathElement(identificationLabel), descriptions);

        registerStatistics(statistics, jpaHibernateRegistration);

        for( final String sublevelChildName : statistics.getChildrenNames()) {
            setupStatistics(statistics.getChildren(sublevelChildName), jpaHibernateRegistration, sublevelChildName, sublevelChildName);
        }


    }

    private static ModelNode describe(Statistics statistics, String descriptionKey, Locale locale) {

        ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(statistics.getDescription(descriptionKey, locale));

        // add attribute descriptions
        for(String statisticName: statistics.getNames()) {
            String description = statistics.getDescription(statisticName, locale);
            if (statistics.isAttribute(statisticName)) {
                subsystem.get(ATTRIBUTES, statisticName,DESCRIPTION).set(description);
                Class type = statistics.getType(statisticName);
                if(Integer.class.equals(type)) {
                    subsystem.get(ATTRIBUTES,statisticName, TYPE).set(ModelType.INT);
                }
                else if(Long.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, statisticName, TYPE).set(ModelType.LONG);
                }
                else if(String.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, statisticName, TYPE).set(ModelType.STRING);
                }
                else if(Boolean.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, statisticName, TYPE).set(ModelType.BOOLEAN);
                }
            }
        }

        // add placeholder for operations
        subsystem.get(OPERATIONS);

        // add children
        for( String sublevelChildName : statistics.getChildrenNames()) {
            subsystem.get(CHILDREN, sublevelChildName, DESCRIPTION).set(sublevelChildName);
            subsystem.get(CHILDREN, sublevelChildName, MODEL_DESCRIPTION); // placeholder
        }

        return subsystem;
    }

    private static ModelNode describeOperation(final Statistics statistics, final String statisticName, final Locale locale) {


           ModelNode modelNode = new ModelNode();

           String description = statistics.getDescription(statisticName, locale);
           modelNode.get(ATTRIBUTES, statisticName, DESCRIPTION).set(description);
           return modelNode;
       }

    private static void registerStatistics(final Statistics statistics, final ManagementResourceRegistration jpaHibernateRegistration) {

        for(final String statisticName: statistics.getNames()) {
            if (statistics.isAttribute(statisticName)) {
                // handle writeable attributes
                if (statistics.isWriteable(statisticName)) {
                    jpaHibernateRegistration.registerReadWriteAttribute(statisticName,
                        new AbstractMetricsHandler() {
                            @Override
                            void handle(final ModelNode response, final String name, OperationContext context) {
                                Object result = statistics.getValue(statisticName);
                                if (result != null) {
                                    response.set(result.toString());
                                }
                            }
                        },
                        new StatisticsEnabledWriteHandler(statistics, statisticName),
                        AttributeAccess.Storage.RUNTIME
                    );

                }
                else {
                    // handle readonly attributes
                    jpaHibernateRegistration.registerMetric(statisticName, new AbstractMetricsHandler() {
                        @Override
                        void handle(final ModelNode response, final String name, OperationContext context) {
                            Object result = statistics.getValue(statisticName);
                            if (result != null) {
                                response.set(result.toString());
                            }
                        }
                    });
                }
            } else if(statistics.isOperation(statisticName)) {

                DescriptionProvider descriptionProvider = new DescriptionProvider() {
                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        return describeOperation(statistics, statisticName, locale);
                    }
                };

                jpaHibernateRegistration.registerOperationHandler(statisticName, new AbstractMetricsHandler() {
                    @Override
                    void handle(final ModelNode response, final String name, OperationContext context) {
                        Object result = statistics.getValue(statisticName);
                        if (result != null) {
                            response.set(result.toString());
                        }
                    }
                }, descriptionProvider);
            }
        }
    }

    private abstract static class AbstractMetricsHandler extends AbstractRuntimeOnlyHandler {

        abstract void handle(final ModelNode response, final String name,  final OperationContext context);

        @Override
        protected void executeRuntimeStep(final OperationContext context, final ModelNode operation) throws
                OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            // final String puResourceName = address.getLastElement().getValue();
            handle(context.getResult(), address.getLastElement().getValue(), context);
            context.stepCompleted();
        }
    }

    /**
     * Attribute write handler for the statistics enabled attribute.
     *
     */
    public static class StatisticsEnabledWriteHandler implements OperationStepHandler {

        private final ParameterValidator validator = new StringLengthValidator(0, Integer.MAX_VALUE, false, false);
        private final Statistics statistics;
        private final String statisticName;

        public StatisticsEnabledWriteHandler(final Statistics statistics, final String statisticName) {
            this.statistics = statistics;
            this.statisticName = statisticName;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

            if (context.isNormalServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                        boolean oldSetting = false;
                        {
                            final ModelNode value = operation.get(ModelDescriptionConstants.VALUE).resolve();
                            validator.validateResolvedParameter(ModelDescriptionConstants.VALUE, value);
                            final boolean setting = value.asBoolean();

                            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                            oldSetting = (Boolean)statistics.getValue(statisticName);
                            statistics.setValue(statisticName,setting);
                        }
                        final boolean rollBackValue = oldSetting;
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                statistics.setValue(statisticName,rollBackValue);
                            }
                        });

                    }
                }, OperationContext.Stage.RUNTIME);
            }

            context.stepCompleted();
        }
    }

}
