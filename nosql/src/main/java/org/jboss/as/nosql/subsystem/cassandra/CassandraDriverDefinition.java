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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * NoSQL ResourceDefinition
 *
 */
public class CassandraDriverDefinition extends SimpleResourceDefinition {

    /* NoSQL profiles */
    static final String PROFILES = "profiles";
    static final String PROFILE = "profile";
    private static final String JNDINAME_NAME = "jndi-name";

    /* NoSQL driver */
    static final String DRIVERS = "drivers";
    static final String DRIVER = "driver";
    private static final String DRIVER_NAME = "driver-name";
    private static final String DRIVER_MODULE_NAME_NAME = "driver-module-name";
    private static final String DRIVER_SETTINGS = "driver-settings";



    public static final CassandraDriverDefinition INSTANCE = new CassandraDriverDefinition();

    private CassandraDriverDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, CassandraDriverExtension.SUBSYSTEM_NAME),
                CassandraDriverExtension.getResourceDescriptionResolver(),
                CassandraDriverSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    static SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(JNDINAME_NAME, ModelType.STRING, false)
            .setXmlName(Attribute.JNDI_NAME.getLocalName())
            .setAllowExpression(true)
            .build();


    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        // define the nosql profile attributes
        // registration.registerReadWriteAttribute();
    }
}
