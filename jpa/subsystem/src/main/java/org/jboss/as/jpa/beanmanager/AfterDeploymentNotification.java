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

package org.jboss.as.jpa.beanmanager;

import java.util.ArrayList;

import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;

/**
 * AfterDeploymentNotification
 *
 * @author Scott Marlow
 */
public class AfterDeploymentNotification {

    private boolean afterDeploymentValidation = false;
    private final ArrayList<DeferredCall> deferredCalls = new ArrayList();


    public synchronized void register(final PersistenceProviderAdaptor persistenceProviderAdaptor, final Object wrapperBeanManagerLifeCycle) {
        if (afterDeploymentValidation) {
            persistenceProviderAdaptor.markPersistenceUnitAvailable(wrapperBeanManagerLifeCycle);
        } else {
            deferredCalls.add(new DeferredCall(persistenceProviderAdaptor, wrapperBeanManagerLifeCycle));
        }
    }

    public synchronized void markPersistenceUnitAvailable() {
        afterDeploymentValidation = true;
        for(DeferredCall deferredCall: deferredCalls) {
            deferredCall.markPersistenceUnitAvailable();
        }
        deferredCalls.clear();
    }

    /**
     * Return current (top level) deployment AfterDeploymentNotification
     * TODO: change hack to lookup current deployment (only good for one deployment at a time currently)
     * @return current top level deployments AfterDeploymentNotification
     */
    private static AfterDeploymentNotification hack = new AfterDeploymentNotification();
    public static AfterDeploymentNotification current() {
        return hack;
    }

    private static class DeferredCall {
        private final PersistenceProviderAdaptor persistenceProviderAdaptor;
        private final Object wrapperBeanManagerLifeCycle;

        DeferredCall(final PersistenceProviderAdaptor persistenceProviderAdaptor, final Object wrapperBeanManagerLifeCycle) {
            this.persistenceProviderAdaptor = persistenceProviderAdaptor;
            this.wrapperBeanManagerLifeCycle = wrapperBeanManagerLifeCycle;
        }

        void markPersistenceUnitAvailable() {
            persistenceProviderAdaptor.markPersistenceUnitAvailable(wrapperBeanManagerLifeCycle);
        }
    }

}
