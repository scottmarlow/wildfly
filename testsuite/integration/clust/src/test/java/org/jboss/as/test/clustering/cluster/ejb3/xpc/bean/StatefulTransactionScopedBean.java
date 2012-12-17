package org.jboss.as.test.clustering.cluster.ejb3.xpc.bean;

import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.jboss.ejb3.annotation.Clustered;

/**
 * StatefulTransactionScopedBean
 *
 * @author Scott Marlow
 */
@Clustered
@javax.ejb.Stateful(name = "StatefulTransactionScopedBean")

public class StatefulTransactionScopedBean implements Stateful {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.TRANSACTION)
        EntityManager em;

    /**
     * Create the employee but don't commit the change to the database, instead keep it in the
     * extended persistence context.
     *
     * @param name
     * @param address
     * @param id
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createEmployee(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
        logStats("createEmployee");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getEmployee(int id) {
        logStats("getEmployee " + id);
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getSecondBeanEmployee(int id) {
        logStats("getSecondBeanEmployee");
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @Remove
    public void destroy() {
        logStats("destroy");
    }

    @Override
        @TransactionAttribute(TransactionAttributeType.REQUIRED)
        public void deleteEmployee(int id) {
            Employee employee = em.find(Employee.class, id, LockModeType.NONE);
            em.remove(employee);
            logStats("deleteEmployee");
        }


    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void flush() {
        logStats("flush");
    }

    @Override
    public void clear() {
        em.clear();
        logStats("clear");
    }

    @Override
    public void clearCache() {
        em.getEntityManagerFactory().getCache().evictAll();
        logStats("clearCache");
    }


    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void echo(String message) {
        System.out.println("echo entered for " + message);
        logStats("echo " + message);
        System.out.println("echo completed for " + message);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int executeNativeSQL(String nativeSql) {
        logStats("executeNativeSQL");
        return em.createNativeQuery(nativeSql).executeUpdate();
    }

    @Override
    public String getVersion() {
        return "none";
    }

    @Override
    public long getEmployeesInMemory() {
        Session session = em.unwrap(Session.class);
        String entityRegionNames[] =  session.getSessionFactory().getStatistics().getSecondLevelCacheRegionNames();
        for (String name: entityRegionNames) {
            if (name.contains(Employee.class.getName())) {
                SecondLevelCacheStatistics stats = session.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(name);
                return stats.getElementCountInMemory();
            }
        }
        return -1;

    }

    private void logStats(String methodName) {
        Session session = em.unwrap(Session.class);
        System.out.println(methodName +") logging statistics for session = " + session);
        session.getSessionFactory().getStatistics().setStatisticsEnabled(true);
        session.getSessionFactory().getStatistics().logSummary();
        String entityRegionNames[] =  session.getSessionFactory().getStatistics().getSecondLevelCacheRegionNames();
        for (String name: entityRegionNames) {
            System.out.println("cache entity region name = " + name);
            SecondLevelCacheStatistics stats = session.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(name);
            System.out.println("2lc for " + name+ ": " + stats.toString());

        }
        // we will want to return the SecondLevelCacheStatistics for Employee

    }

}
