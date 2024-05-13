/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction.lazyconnectionenlistment;

import java.sql.Connection;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateless
public class TestAPCBean {
    @PersistenceContext(unitName = "pu1")
    EntityManager em;
    @EJB
    private ActivityNamesBean activityNamesBean;
    @Resource
    private javax.sql.DataSource dataSource;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void startUserTransactionGetConnectionCallOtherBean() {
        try {
            // simulate conditions described in https://issues.redhat.com/browse/WFLY-19082?focusedId=24568213&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-24568213
            // hold database connection and do some database lookups without an active transaction
            Connection connection = dataSource.getConnection();
            System.out.println("hold database connection and do some database lookups without an active transaction.  Connection = " + connection);
            em.find(Employee.class, 1); // will not find Employee 1 which is expected
            em.find(Employee.class, 2); // will not find Employee 2 which is expected
            activityNamesBean.lookup();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }
    }
}
