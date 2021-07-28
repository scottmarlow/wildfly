/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.deployment.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.spi.DeploymentUnitDependenciesProvider;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * @author Martin Kouba
 */
public class JpaDependenciesProvider implements DeploymentUnitDependenciesProvider {

    @Override
    public Set<ServiceName> getDependencies(DeploymentUnit deploymentUnit) {
        Set<ServiceName> dependencies = new HashSet<>();
        EarMetaData earConfig = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        if (earConfig != null && earConfig.getInitializeInOrder() && earConfig.getModules().size() > 1) {
            // WFLY-14923 add persistence units defined in current (sub) deployment unit to EE components
            // also in current deployment unit.
            if (deploymentUnit.getParent() != null) {
/**
 * Do not add WeldStartService dependencies on persistence units contained in sub-deployments (modules).
                List<PersistenceUnitMetadata> collectPersistenceUnitsForCurrentDeploymentUnit = new ArrayList<>();
                final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                final ModuleMetaData moduleMetaData = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.MODULE_META_DATA);
                PersistenceUnitsInApplication persistenceUnitsInApplication = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);
                for (PersistenceUnitMetadataHolder holder : persistenceUnitsInApplication.getPersistenceUnitHolders()) {
                    if (holder != null && holder.getPersistenceUnits() != null) {
                        for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                            String moduleName = pu.getContainingModuleName().get(pu.getContainingModuleName().size() - 1);
                            if (moduleName.equals(moduleMetaData.getFileName())) {
                                final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                                dependencies.add(serviceName);
                            }
                        }
                    }
                }
 **/
            } else {
                // WFLY-14923
                // add Jakarta EE component dependencies on all persistence units in top level deployment unit.
                List<ResourceRoot> resourceRoots = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachmentList(Attachments.RESOURCE_ROOTS);
                for (ResourceRoot resourceRoot : resourceRoots) {
                    // look at resources that aren't subdeployments
                    if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                        final PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                        for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                            String jpaContainerManaged = pu.getProperties().getProperty(Configuration.JPA_CONTAINER_MANAGED);
                            boolean deployPU = (jpaContainerManaged == null ? true : Boolean.parseBoolean(jpaContainerManaged));
                            if (deployPU) {
                                final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                                dependencies.add(serviceName);
                            }
                        }
                    }
                }
            }
        } else {
            for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                final PersistenceUnitMetadataHolder persistenceUnits = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (persistenceUnits != null && persistenceUnits.getPersistenceUnits() != null) {
                    for (final PersistenceUnitMetadata pu : persistenceUnits.getPersistenceUnits()) {
                        final Properties properties = pu.getProperties();
                        final String jpaContainerManaged = properties.getProperty(Configuration.JPA_CONTAINER_MANAGED);
                        final boolean deployPU = (jpaContainerManaged == null || Boolean.parseBoolean(jpaContainerManaged));
                        if (deployPU) {
                            final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                            dependencies.add(serviceName);
                        }
                    }
                }
            }

        }
        return dependencies;
    }
}
