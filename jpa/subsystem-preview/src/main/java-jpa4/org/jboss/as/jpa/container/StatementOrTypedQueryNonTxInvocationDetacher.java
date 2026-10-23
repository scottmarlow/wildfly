/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import jakarta.persistence.EntityManager;
import jakarta.persistence.StatementOrTypedQuery;

/**
 * Helper class to create StatementOrTypedQuery detachers for JPA 4.0.
 *
 * @author Scott Marlow
 */
public class StatementOrTypedQueryNonTxInvocationDetacher {

    /**
     * Creates a StatementOrTypedQuery wrapper that detaches entities on non-tx invocations.
     */
    public static StatementOrTypedQuery create(EntityManager underlyingEntityManager, StatementOrTypedQuery underlyingQuery) {
        // Reuse QueryNonTxInvocationDetacher since it now implements StatementOrTypedQuery
        return (StatementOrTypedQuery) QueryNonTxInvocationDetacher.create(underlyingEntityManager, underlyingQuery);
    }

    private StatementOrTypedQueryNonTxInvocationDetacher() {
        // utility class
    }
}
