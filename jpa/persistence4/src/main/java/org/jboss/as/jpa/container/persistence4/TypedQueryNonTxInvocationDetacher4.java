/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.as.jpa.container.TypedQueryNonTxInvocationDetacher;

/**
 * JPA 4.0 concrete factory implementation for TypedQueryNonTxInvocationDetacher.
 * All JPA 4.0 methods are now in the base class.
 */
public class TypedQueryNonTxInvocationDetacher4<X> extends TypedQueryNonTxInvocationDetacher<X> {

    protected TypedQueryNonTxInvocationDetacher4(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
    }
}
