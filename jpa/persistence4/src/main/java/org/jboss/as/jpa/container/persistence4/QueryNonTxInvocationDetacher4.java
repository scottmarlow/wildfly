/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence4;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.jboss.as.jpa.container.QueryNonTxInvocationDetacher;

/**
 * JPA 4.0 concrete factory implementation for QueryNonTxInvocationDetacher.
 * All JPA 4.0 methods are now in the base class.
 */
final class QueryNonTxInvocationDetacher4 extends QueryNonTxInvocationDetacher {

    QueryNonTxInvocationDetacher4(EntityManager underlyingEntityManager, Query underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
    }
}
