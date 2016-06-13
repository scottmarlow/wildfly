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

package org.wildfly.extension.nosql.subsystem.mongodb;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of write-concern configuration.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class WriteConcernDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition W = new SimpleAttributeDefinitionBuilder(CommonAttributes.W, ModelType.STRING, true)
            .setAllowExpression(true).build();

    static final SimpleAttributeDefinition J = new SimpleAttributeDefinitionBuilder(CommonAttributes.J, ModelType.BOOLEAN, true)
            .setAllowExpression(true).build();

    static final SimpleAttributeDefinition WTIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.WTIMEOUT,
            ModelType.INT, true).setAllowExpression(true).build();

    private static final List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(W, J, WTIMEOUT);

    private static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }
    }

    static final WriteConcernDefinition INSTANCE = new WriteConcernDefinition();

    private WriteConcernDefinition() {
        super(MongoDriverExtension.WRITE_CONCERN_PATH, MongoDriverExtension.getResolver(CommonAttributes.WRITE_CONCERN),
                WriteConcernAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    private static class WriteConcernAdd extends AbstractAddStepHandler {
        private static final WriteConcernAdd INSTANCE = new WriteConcernAdd();

        private WriteConcernAdd() {
            super(ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
        }
    }

}
