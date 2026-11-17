/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.el.ELAwareBeanManager;
import org.jboss.weld.util.ForwardingBeanManager;

/**
 * Proxy for Jakarta Contexts and Dependency Injection BeanManager
 *
 * @author Scott Marlow
 */
public class ProxyBeanManager extends ForwardingBeanManager {

    private volatile ELAwareBeanManager delegate;

    @Override
    public ELAwareBeanManager delegate() {
        return delegate;
    }

    @Override
    public <T> T unwrapClientProxy(T instance) {
        return delegate().unwrapClientProxy(instance);
    }

    public void setDelegate(BeanManager delegate) {
        // The shared bootstrap uses BeanManager; Preview's CDI 5 delegate also supplies EL operations.
        this.delegate = (ELAwareBeanManager) delegate;
    }
}
