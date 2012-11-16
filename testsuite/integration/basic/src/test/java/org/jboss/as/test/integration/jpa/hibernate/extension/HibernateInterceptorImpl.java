package org.jboss.as.test.integration.jpa.hibernate.extension;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

/**
 * HibernateInterceptorImpl
 *
 * @author Scott Marlow
 */
public class HibernateInterceptorImpl implements Interceptor {

    public static int getInvokeCount() {
        return invokeCount.get();
    }

    static private AtomicInteger invokeCount = new AtomicInteger();

    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        invokeCount.getAndIncrement();
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        invokeCount.getAndIncrement();
        return false;
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        invokeCount.getAndIncrement();
        return false;
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        invokeCount.getAndIncrement();
    }

    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        invokeCount.getAndIncrement();
    }

    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        invokeCount.getAndIncrement();
    }

    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        invokeCount.getAndIncrement();
    }

    @Override
    public void preFlush(Iterator entities) throws CallbackException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void postFlush(Iterator entities) throws CallbackException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Boolean isTransient(Object entity) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getEntityName(Object object) throws CallbackException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getEntity(String entityName, Serializable id) throws CallbackException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        invokeCount.getAndIncrement();
    }

    @Override
    public void beforeTransactionCompletion(Transaction tx) {
        invokeCount.getAndIncrement();
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        invokeCount.getAndIncrement();
    }

    @Override
    public String onPrepareStatement(String sql) {
        invokeCount.getAndIncrement();
        return sql;
    }
}
