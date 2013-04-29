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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jipijapa.management.spi.Statistics;
import org.jipijapa.plugin.spi.ManagementAdaptor;

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


                final Statistics statistics = managementAdaptor.getStatistics();
                // setup statistics (this used to be part of JPA subsystem startup)
                ResourceDescriptionResolver resourceDescriptionResolver = new StandardResourceDescriptionResolver(
                        statistics.getResourceBundleKeyPrefix(), statistics.getResourceBundleName(), statistics.getClass().getClassLoader());

                jpaSubsystemDeployments.registerSubModel(
                        new ManagementResourceDefinition(PathElement.pathElement(managementAdaptor.getIdentificationLabel()), resourceDescriptionResolver, statistics, entityManagerFactoryLookup));

                // create dynamic Resource implementation that can reflect the deployment specific names (e.g. jpa entity classname/Hibernate region name)
                result = new DynamicManagementStatisticsResource(statistics, scopedPersistenceUnitName, managementAdaptor.getIdentificationLabel(), entityManagerFactoryLookup);

                existingManagementStatisticsResource.put(managementAdaptor.getVersion(),result);

            }
            return result;
        }
    }
}
