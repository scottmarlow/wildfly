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

package org.jboss.as.jpa.transaction;

import static org.jboss.as.jpa.messages.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.messages.JpaMessages.MESSAGES;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.AbstractEntityManager;
import org.jboss.as.jpa.container.EntityManagerUtil;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.tm.TxUtils;

/**
 * Transaction utilities for JPA
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {

    private static volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static volatile TransactionManager transactionManager;
    private static final String ARJUNA_REAPER_THREAD_NAME = "Transaction Reaper Worker";

    public static void setTransactionManager(TransactionManager tm) {
        if (transactionManager == null) {
            transactionManager = tm;
        }
    }

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        if (TransactionUtil.transactionSynchronizationRegistry == null) {
            TransactionUtil.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        }
    }

    public static boolean isInTx() {
        Transaction tx = getTransaction();
        if (tx == null || !TxUtils.isActive(tx))
            return false;
        return true;
    }

    /**
     * Register the specified entity manager (persistence context) with the current transaction.
     * Precondition:  Only call while a transaction is active in the current thread.
     *
     * @param scopedPuName            is the fully (application deployment) scoped name of persistence unit
     * @param xpc                     is the entity manager (a org.jboss.as.jpa class) to register
     * @param underlyingEntityManager is the underlying entity manager obtained from the persistence provider
     */
    public static void registerExtendedUnderlyingWithTransaction(String scopedPuName, EntityManager xpc, EntityManager underlyingEntityManager) {
        // xpc invoked this method, we cannot call xpc because it will recurse back to here, join with underloying em instead
        registerSynchronization(xpc, scopedPuName, false);
        underlyingEntityManager.joinTransaction();
        putEntityManagerInTransactionRegistry(scopedPuName, xpc);
    }

    /**
     * Get current persistence context.  Only call while a transaction is active in the current thread.
     *
     * @param puScopedName
     * @return
     */
    public static EntityManager getTransactionScopedEntityManager(String puScopedName) {
        return getEntityManagerInTransactionRegistry(puScopedName);
    }

    /**
     * Get current PC or create a Transactional entity manager.
     * Only call while a transaction is active in the current thread.
     *
     * @param emf
     * @param scopedPuName
     * @param properties
     * @param synchronizationType
     * @return
     */
    public static EntityManager getOrCreateTransactionScopedEntityManager(
            final EntityManagerFactory emf, final String scopedPuName, final Map properties, final SynchronizationType synchronizationType) {
        EntityManager entityManager = getEntityManagerInTransactionRegistry(scopedPuName);
        if (entityManager == null) {
            entityManager = EntityManagerUtil.createEntityManager(emf, properties, synchronizationType);
            if (JPA_LOGGER.isDebugEnabled())
                JPA_LOGGER.debugf("%s: created entity manager session %s", getEntityManagerDetails(entityManager),
                    getTransaction().toString());
            boolean autoCloseEntityManager = true;
            registerSynchronization(entityManager, scopedPuName, autoCloseEntityManager);
            putEntityManagerInTransactionRegistry(scopedPuName, entityManager);
        } else {
            testForMixedSyncronizationTypes(entityManager, scopedPuName, synchronizationType);
            if (JPA_LOGGER.isDebugEnabled()) {
                JPA_LOGGER.debugf("%s: reuse entity manager session already in tx %s", getEntityManagerDetails(entityManager),
                    getTransaction().toString());
            }
        }
        return entityManager;
    }

    private static void registerSynchronization(EntityManager entityManager, String puScopedName, boolean closeEMAtTxEnd) {
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(new SessionSynchronization(entityManager, closeEMAtTxEnd, puScopedName));
    }

    private static Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw MESSAGES.errorGettingTransaction(e);
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }


    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    private static String getEntityManagerDetails(EntityManager manager) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        if (manager instanceof ExtendedEntityManager) {
            result += manager.toString();
        }
        else {
            result += "[transaction scoped EntityManager]";
        }
        return result;
    }

    /**
     * throw error if jta transaction already has an UNSYNCHRONIZED persistence context and a SYNCHRONIZED persistence context
     * is requested.  We are only fussy in this test, if the target component persistence context is SYNCHRONIZED.
     */
    private static void testForMixedSyncronizationTypes(EntityManager entityManager, String scopedPuName, final SynchronizationType targetSynchronizationType) {
        if (SynchronizationType.SYNCHRONIZED.equals(targetSynchronizationType)
                && entityManager instanceof AbstractEntityManager
                && SynchronizationType.UNSYNCHRONIZED.equals( ((AbstractEntityManager)entityManager).getSynchronizationType())) {
            throw MESSAGES.badSynchronizationTypeCombination(scopedPuName);
        }
    }

    private static EntityManager getEntityManagerInTransactionRegistry(String scopedPuName) {
        return  (EntityManager)getTransactionSynchronizationRegistry().getResource(scopedPuName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    private static void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager) {
        getTransactionSynchronizationRegistry().putResource(scopedPuName, entityManager);
    }

    private static class SessionSynchronization implements Synchronization {
        private EntityManager manager;  // the underlying entity manager
        private boolean closeAtTxCompletion;
        private String scopedPuName;

        public SessionSynchronization(EntityManager session, boolean close, String scopedPuName) {
            this.manager = session;
            closeAtTxCompletion = close;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            /**
             * If its not safe (safeToClose returns false) to close the EntityManager now,
             * any connections joined to the JTA transaction
             * will be released by the JCA connection pool manager.  When the JTA Transaction is no longer
             * referencing the EntityManager, it will be eligible for garbage collection.
             * See AS7-6586 for more details.
             */
            if (closeAtTxCompletion && safeToClose(status)) {
                try {
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf("%s: closing entity managersession", getEntityManagerDetails(manager));
                    manager.close();
                } catch (Exception ignored) {
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf(ignored, "ignoring error that occurred while closing EntityManager for %s (", scopedPuName);
                }
            }
            // The TX reference to the entity manager, should be cleared by the TM

        }

        /**
         * AS7-6586 requires that the container avoid closing the EntityManager while the application
         * may be using the EntityManager in a different thread.  If the transaction has been rolled
         * back, will check if the current thread is the Arjuna transaction manager Reaper thread.  It is not
         * safe to call EntityManager.close from the Reaper thread, so false is returned.
         *
         * TODO: switch to depend on JBTM-1556 instead of checking the current thread name.
         *
         * @param status of transaction.
         * @return
         */
        private boolean safeToClose(int status) {
            boolean isItSafe = true;
            if (Status.STATUS_COMMITTED != status) {
                String currentThreadName = currentThread();
                boolean isBackgroundReaperThread = currentThreadName != null &&
                        currentThreadName.startsWith(ARJUNA_REAPER_THREAD_NAME);
                isItSafe = !isBackgroundReaperThread;
            }
            return isItSafe;
        }
    }


}
