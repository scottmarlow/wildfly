/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2016, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.jpa.beanmanager;

import java.util.ArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

/**
 * BeanManagerAfterDeploymentValidation
 *
 * @author Scott Marlow
 */
public class BeanManagerAfterDeploymentValidation implements Extension {

    public BeanManagerAfterDeploymentValidation(boolean afterDeploymentValidation) {
        this.afterDeploymentValidation = afterDeploymentValidation;
    }

    public BeanManagerAfterDeploymentValidation() {
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
        this.cdiBeanManager = manager;
        try {
            runNow();
        } catch (Throwable throwable) {
            event.addDeploymentProblem(throwable);
        }
    }

    private synchronized void runNow() {
        afterDeploymentValidation = true;
        for(DeferredCall deferredCall: deferredCalls) {
            deferredCall.runNow(cdiBeanManager);
        }
        deferredCalls.clear();
    }

    private boolean afterDeploymentValidation = false;
    private final  ArrayList<DeferredCall> deferredCalls = new ArrayList();
    private volatile BeanManager cdiBeanManager;

    public synchronized void register(Runnable afterDeploymentValidationTask, ProxyBeanManager proxyBeanManager) {
        if (afterDeploymentValidation) {
            DeferredCall deferredCall = new DeferredCall(afterDeploymentValidationTask, proxyBeanManager);
            deferredCall.runNow(cdiBeanManager);
        } else {
            deferredCalls.add(new DeferredCall(afterDeploymentValidationTask, proxyBeanManager));
        }
    }

    private static class DeferredCall {
        private final Runnable afterDeploymentValidationTask;
        private final ProxyBeanManager beanManager;

        DeferredCall(final Runnable afterDeploymentValidationTask, final ProxyBeanManager beanManager) {
            this.afterDeploymentValidationTask = afterDeploymentValidationTask;
            this.beanManager = beanManager;
        }

        void runNow(BeanManager cdiBeanManager) {
            if (afterDeploymentValidationTask != null && beanManager != null) {
                beanManager.setDelegate(cdiBeanManager);
                afterDeploymentValidationTask.run();
            }
        }
    }

}
