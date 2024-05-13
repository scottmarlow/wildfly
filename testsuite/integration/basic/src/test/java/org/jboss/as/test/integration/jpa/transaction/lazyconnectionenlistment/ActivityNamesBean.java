/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction.lazyconnectionenlistment;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ActivityNamesBean {
    @PersistenceContext(unitName = "pu1")
    EntityManager em;


    public void lookup() {
        System.out.println("ActivityNamesBean about to call EntityManager.find");
        em.find(Employee.class, 3); // will not find Employee 3 which is expected
        System.out.println("ActivityNamesBean about to return");
    }
}
