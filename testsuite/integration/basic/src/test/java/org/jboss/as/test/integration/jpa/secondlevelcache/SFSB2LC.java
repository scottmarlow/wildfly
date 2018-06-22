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

package org.jboss.as.test.integration.jpa.secondlevelcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * SFSB for Second level cache tests
 *
 * @author Zbynek Roubalik
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSB2LC {
    @PersistenceUnit(unitName = "mypc")
    EntityManagerFactory emf;

    /**
     * Checking entity 2LC in one EntityManager session
     */
    // test passes without a JTA tx, need to understand why and what this test should be changed to do and what
    // the Hibernate ORM migration doc should state about this.
    // @TransactionAttribute(TransactionAttributeType.NEVER)
    public String sameSessionCheck(String CACHE_REGION_NAME) {

        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.clear();
        SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME + "Employee");

        try {
            // add new entities and check if they are put in 2LC
            createEmployee(em, "Peter", "Ostrava", 2);
            createEmployee(em, "Tom", "Brno", 3);
            assertEquals("There are 2 puts in the 2LC" + generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getPutCount());

            // loading all Employee entities should put in 2LC all Employee
            List<?> empList = getAllEmployeesQuery(em);
            assertEquals("There are 2 entities.", empList.size(), 2);
            assertEquals("There are 2 entities in the 2LC (according to getElementCountInMemory), getElementCountInMemory=" +emp2LCStats.getElementCountInMemory() +":" + generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getElementCountInMemory());

            // clear session
            em.clear();

            // entity should be loaded from 2L cache, we'are expecting hit in 2L cache
            Employee emp = getEmployee(em, 2);
            assertNotNull("Employee returned", emp);
            assertEquals("Expected 1 hit in cache" + generateEntityCacheStats(emp2LCStats), 1, emp2LCStats.getHitCount());

        } catch (AssertionError e) {
            return e.getMessage();
        } finally {
            em.close();
        }
        return "OK";
    }

    /**
     * Generate entity cache statistics for put, hit and miss count as one String
     */
    public String generateEntityCacheStats(SecondLevelCacheStatistics stats) {
        String result = "(hitCount=" + stats.getHitCount()
                + ", missCount=" + stats.getMissCount()
                + ", putCount=" + stats.getPutCount() + ").";

        return result;
    }


    public String getCacheRegionName() {

        return (String) emf.getProperties().get("hibernate.cache.region_prefix");
    }

    /**
     * Create employee in provided EntityManager
     */
    public void createEmployee(EntityManager em, String name, String address,
                               int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        try {
            em.persist(emp);
            em.flush();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting employee entity", e);
        }
    }

    /**
     * Create employee in provided EntityManager
     */
    public void createEmployee(String name, String address,
                               int id) {
        EntityManager em = emf.createEntityManager();
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        try {
            em.persist(emp);
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting employee entity", e);
        } finally {
            em.close();
        }
    }


    /**
     * Load employee from provided EntityManager
     */
    public Employee getEmployee(EntityManager em, int id) {
        Employee emp = em.find(Employee.class, id);
        return emp;
    }


    /**
     * Load all employees using Query from provided EntityManager
     */
    @SuppressWarnings("unchecked")
    public List<Employee> getAllEmployeesQuery(EntityManager em) {

        Query query;

        query = em.createQuery("from Employee");
        query.setHint("org.hibernate.cacheable", true);

        return query.getResultList();
    }

}
