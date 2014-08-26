/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.transaction.delist;

import javax.annotation.Resource;
import javax.ejb.BeforeCompletion;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {
    @PersistenceContext(unitName = "mypc2")
        EntityManager em2;

    @PersistenceContext(unitName = "mypc1")
        EntityManager em1;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    /**
     * Both persistence units changes should be written to the database via two persistence units and
     * then the transaction is marked roll back only.  After we can test that both changes were rolled back.
     */
    public void createTwoEmployees(String name1, String address1, int id1, String name2, String address2, int id2) {
        Employee emp1 = new Employee();

        emp1.setId(id1);
        emp1.setAddress(address1);
        emp1.setName(name1);
        em1.persist(emp1);

        Employee emp2 = new Employee();
        emp2.setId(id2);
        emp2.setAddress(address2);
        emp2.setName(name2);
        em2.persist(emp2);
    }

    @BeforeCompletion
    public void before() {
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Sync());
    }

    public Employee getEmployee(int id) {
        return em2.find(Employee.class, id, LockModeType.NONE);
    }

    private class Sync implements Synchronization {

        public void beforeCompletion() {
            transactionSynchronizationRegistry.setRollbackOnly();
        }

        public void afterCompletion(int status) {

        }
    }
}
