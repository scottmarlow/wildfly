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
package org.jboss.as.jpa.hibernate4;


import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.hibernate.service.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.service.jta.platform.internal.SynchronizationRegistryAccess;
import org.hibernate.service.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;
import org.jboss.as.jpa.spi.JtaManager;


/**
 * @author Steve Ebersole
 */
public class JBossAppServerJtaPlatform extends org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform {

    private final JtaSynchronizationStrategy synchronizationStrategy;
    private final JtaManager jtaManager;

    public JBossAppServerJtaPlatform(final JtaManager jtaManager) {
        this.jtaManager = jtaManager;
        this.synchronizationStrategy = new SynchronizationRegistryBasedSynchronizationStrategy(new SynchronizationRegistryAccess() {
            @Override
            public TransactionSynchronizationRegistry getSynchronizationRegistry() {
                return jtaManager.getSynchronizationRegistry();
            }
        });
    }

    @Override
    protected boolean canCacheTransactionManager() {
        return true;
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return jtaManager.locateTransactionManager();
    }

    @Override
    protected JtaSynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return jtaManager.getUserTransaction();
    }

}
