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

package org.jboss.as.jpa.hibernate5.management;

import java.util.ArrayList;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;

/**
 * HibernateExtendedBeanManager helps with registering the CDI BeanManager after the persistence unit is available
 * for lookup by the CDI bean code.
 * This solves the WFLY-2387 issue of JPA referencing the CDI bean which cycles back to the persistence unit.
 *
 * Not intended to be thread safe as we know that the only calls are from the same thread (
 * during the PersistenceProvider.createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) call).
 *
 * @author Scott Marlow
 */
public class HibernateExtendedBeanManager implements ExtendedBeanManager{

    private final ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();
    private final BeanManager beanManager;

    public HibernateExtendedBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Hibernate calls registerLifecycleListener to register N callbacks to be notified
     * when the CDI BeanManager can safely be used.  The CDI BeanManager can safely be used
     * after the PersistenceUnitService is started.
     * @param lifecycleListener
     */
    @Override
    public void registerLifecycleListener(LifecycleListener lifecycleListener) {
        lifecycleListeners.add(lifecycleListener);
    }

    public void beanManagerIsAvailableForUse() {
        for (LifecycleListener hibernateCallback:lifecycleListeners) {
            hibernateCallback.beanManagerInitialized(beanManager);
        }
    }
}
