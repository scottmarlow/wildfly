/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jpa.txtimeout;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Resource;
import javax.ejb.EJBTransactionRolledbackException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.tm.listener.EventType;
import org.jboss.tm.listener.TransactionEvent;
import org.jboss.tm.listener.TransactionListener;
import org.jboss.tm.listener.TransactionListenerRegistry;
import org.jboss.tm.listener.TransactionTypeNotSupported;

/**
 * @author Scott Marlow
 *
 */

@WebServlet(name="SimpleServlet", urlPatterns={"/simple"})
public class SimpleServlet extends HttpServlet implements TransactionListener, Synchronization {

    @Resource
    private UserTransaction userTransaction;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    EntityManager entityManager;

    private TransactionSynchronizationRegistry tsr;

    private transient boolean disassocCalled = false;
    private transient boolean acCalled = false;

    private void setupListener() {
        TransactionListenerRegistry transactionListenerRegistry = (TransactionListenerRegistry)com.arjuna.ats.jta.TransactionManager.transactionManager();
        try {
            transactionListenerRegistry.addListener(com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction(),this);
            tsr = new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple();
            tsr.registerInterposedSynchronization(this);
        } catch (TransactionTypeNotSupported transactionTypeNotSupported) {
            transactionTypeNotSupported.printStackTrace();
        } catch (SystemException e) {
            e.printStackTrace();
        }
    }

    /* Synchronization methods */
    @Override
    public void beforeCompletion() {
        System.out.println("xxxxx Synchronization.beforeCompletion");
        acCalled = false;
    }

    @Override
    public synchronized void afterCompletion(int status) {
        System.out.println("xxxxx Synchronization.afterCompletion(" + status + ")");
        acCalled = true;
        if (safeToClose()) {
            System.out.println("xxx safe to close resources after completion.  Clearing entityManager");
            entityManager.clear();
        }
    }

    private boolean safeToClose() {

      return acCalled == true && disassocCalled == true;

    }
    /* TransactionListener methods */
    @Override
    public synchronized void onEvent(TransactionEvent transactionEvent) {
        if (transactionEvent.getType().equals(EventType.ASSOCIATED)) {
            System.out.println("xxxxx onEvent(TransactionEvent entered associated with transaction =" + transactionEvent.getTransaction());
            disassocCalled = false;
        }
        else {
            System.out.println("xxxxx onEvent(TransactionEvent entered disassociated with transaction =" + transactionEvent.getTransaction());
            disassocCalled = true;
            if (safeToClose()) {
                System.out.println("xxx safe to close resources after disassociated.  Clearing entityManager");
                entityManager.clear();
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();

        int entityCount = 0;
        try {
            int timeoutSeconds = 5;
            userTransaction.setTransactionTimeout(timeoutSeconds);
            userTransaction.begin();
            setupListener();
            entityManager = entityManagerFactory.createEntityManager();
            System.out.println("entityManager.isJoinedToTransaction() == " + entityManager.isJoinedToTransaction());
            entityManager.joinTransaction();
            System.out.println("entityManager.isJoinedToTransaction() == " + entityManager.isJoinedToTransaction());

            boolean notRolledBackException = false;
            while (!notRolledBackException) {
                try {
                    Thread.sleep(10 * 1000);
                    int transactionStatus = userTransaction.getStatus();
                    System.out.println("after ten second sleep (tx should of rolled back by now), application transaction status = " + transactionStatus );
                    if ( transactionStatus == Status.STATUS_ROLLEDBACK) {
                        /**
                         * Simulate race condition that could happen if background transaction rollback (and persistence provider clearing of
                         * persistence context) occurs before the application thread is about to add an entity (after the transaction rolled back in the background).
                         * What should the application expect to happen in this case?
                         */
                        entityCount++;
                        createEmployee(entityManager, "name" + entityCount, "address" + entityCount, entityCount);
                        if (getEmployee(entityManager, entityCount) != null) {
                            System.out.println("persistence context contains invalid state, as last written entity was not detached. will rollback tx and check again");
                            try {
                                System.out.println("about to call userTransaction.rollback()");
                                userTransaction.rollback();
                            } catch (SystemException e) {
                                e.printStackTrace();
                            }
                            if (getEmployee(entityManager, entityCount) != null) {
                                writer.write("failed as entity " + entityCount + ", was added after background transaction rollback simulation");
                                writer.flush();
                                writer.close();
                                return;
                            }
                            else {
                                // loop again, start new tx
                                userTransaction.begin();
                                setupListener();
                                entityManager.joinTransaction();
                            }
                        }

                    }
                } catch (Exception exception) {
                    if (exception instanceof EJBTransactionRolledbackException) {
                        System.out.println("ignoring expected RollbackException by repeating invocation, count=" + entityCount);
                    } else {
                        System.out.println("caught exception that we didn't expect: " + exception.getClass().getName() + ", " + exception.getMessage() +
                                ", count=" + entityCount);

                        notRolledBackException = true;
                    }
                    // try again until its not RollbackException:
                }
            }
        } catch (NotSupportedException e) {
            e.printStackTrace();
        } catch (SystemException e) {
            e.printStackTrace();
        }
        writer.write("success");
    }

    public void createEmployee(EntityManager entityManager, String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        entityManager.persist(emp);
    }

    public Employee getEmployee(EntityManager entityManager, int id) {

        return entityManager.find(Employee.class, id);
    }

}
