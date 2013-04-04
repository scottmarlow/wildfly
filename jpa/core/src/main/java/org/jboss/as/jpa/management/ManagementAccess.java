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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;

import java.util.Locale;

import javax.persistence.EntityManagerFactory;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.processor.PersistenceProviderAdaptorLoader;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleLoadException;
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

    private static ManagementResourceRegistration jpaSubsystemDeployments;

    public static void setManagementResourceRegistration(ManagementResourceRegistration managementResourceRegistration) {
        jpaSubsystemDeployments = managementResourceRegistration;
        // setup();
    }

    public static void setup(EntityManagerFactory entityManagerFactory) {
        try {
            // load the default persistence provider adaptor
            PersistenceProviderAdaptor provider = PersistenceProviderAdaptorLoader.loadPersistenceAdapterModule(Configuration.ADAPTER_MODULE_DEFAULT);
            final ManagementAdaptor managementAdaptor = provider.getManagementAdaptor();
            if (managementAdaptor != null) {
                if( managementAdaptor.getStatisticsPlugin(entityManagerFactory) != null) {
                    setupNewWay(managementAdaptor, entityManagerFactory);
                }
                else {
                    managementAdaptor.register(jpaSubsystemDeployments, PersistenceUnitRegistryImpl.INSTANCE);
                }
            }
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProviderAdaptor(e);
        }

    }

    public static void setupNewWay(final ManagementAdaptor managementAdaptor, EntityManagerFactory entityManagerFactory) {
        final StatisticsPlugin statisticsPlugin = managementAdaptor.getStatisticsPlugin(entityManagerFactory);
        // setup top level statistics
        DescriptionProvider topLevelDescriptions = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                // get description/type for each top level Hibernate statistic
                return describeTopLevelAttributes(statisticsPlugin, locale);
            }
        };

        final ManagementResourceRegistration jpaHibernateRegistration =
            jpaSubsystemDeployments.registerSubModel(PathElement.pathElement(managementAdaptor.getIdentificationLabel()), topLevelDescriptions);
        /*
        registerStatisticAttributes(jpaHibernateRegistration);

        registerStatisticOperations(jpaHibernateRegistration);

        jpaHibernateRegistration.registerSubModel(new SecondLevelCacheResourceDefinition(persistenceUnitRegistry));
        jpaHibernateRegistration.registerSubModel(new QueryResourceDefinition(persistenceUnitRegistry));
        jpaHibernateRegistration.registerSubModel(new EntityResourceDefinition(persistenceUnitRegistry));
        jpaHibernateRegistration.registerSubModel(new CollectionResourceDefinition(persistenceUnitRegistry));
        */

    }

    private static ModelNode describeTopLevelAttributes(StatisticsPlugin statisticsPlugin, Locale locale) {

        Statistics statistics = statisticsPlugin.getStatistics();
        ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(statistics.getDescription(statisticsPlugin.TOPLEVEL_DESCRIPTION_RESOURCEBUNDLE_KEY, locale));
        for(String statisticName: statistics.getNames()) {
            String description = statistics.getDescription(statisticName, locale);
            if (statistics.isAttribute(statisticName)) {
                subsystem.get(ATTRIBUTES, statisticName,DESCRIPTION).set(description);
                Class type = statistics.getType(statisticName);
                if(Integer.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, TYPE).set(ModelType.INT);
                }
                else if(String.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, TYPE).set(ModelType.STRING);
                }
                else if(Boolean.class.equals(type)) {
                    subsystem.get(ATTRIBUTES, TYPE).set(ModelType.BOOLEAN);
                }
            }

        }

        /*
        subsystem.get(OPERATIONS);  // placeholder

        subsystem.get(CHILDREN, "entity-cache", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE));
        subsystem.get(CHILDREN, "entity-cache", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "query-cache", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_STATISTICS));
        subsystem.get(CHILDREN, "query-cache", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "entity", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_STATISTICS));
        subsystem.get(CHILDREN, "entity", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "collection", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_STATISTICS));
        subsystem.get(CHILDREN, "collection", MODEL_DESCRIPTION); // placeholder
        */
        return subsystem;
    }

    private static Resource createPersistenceUnitResource() {
        return new ManagementStatisticsResource();
    }


}
