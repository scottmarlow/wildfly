/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Remove default datasource
 *
 * @author Scott Marlow
 */
public class JPADataSourceRemove implements ModelUpdateOperationHandler {

    static final JPADataSourceRemove INSTANCE = new JPADataSourceRemove();
    static final String OPERATION_NAME = "remove-datasource";

    private JPADataSourceRemove() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode subModel = context.getSubModel();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(JPADataSourceAdd.OPERATION_NAME);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(CommonAttributes.DEFAULT_DATASOURCE).set(subModel.get(CommonAttributes.DEFAULT_DATASOURCE));

        subModel.get(CommonAttributes.DEFAULT_DATASOURCE).clear();

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> service = context.getServiceRegistry().getService(JPAService.SERVICE_NAME);
                    if (service != null) {
                        service.setMode(ServiceController.Mode.REMOVE);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }
}


