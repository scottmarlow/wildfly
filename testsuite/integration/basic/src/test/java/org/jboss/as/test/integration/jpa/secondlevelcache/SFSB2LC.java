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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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
	 * Insert 2 entities and put them into the 2LC and then evicts entity cache.
	 */
	public String addEntities(String CACHE_REGION_NAME){

		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME+"Employee");
		
		try{
			createEmployee(em, "Jan", "Ostrava", 20);
			createEmployee(em, "Martin", "Brno", 30);
			assertEquals("There are 2 puts in the 2LC"+generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getPutCount());
			
			assertTrue("Expected entities stored in the cache"+generateEntityCacheStats(emp2LCStats), emp2LCStats.getElementCountInMemory() > 0);
            //em.flush();
		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		
		return "OK";
	}

	
	/**
	 * evict 2lc cache and checks cache to verify it was cleared.
	 */
	public String evictEntityCacheCheck(String CACHE_REGION_NAME){

        assertTrue("2lc entity cache is expected to contain Employee id = 20", emf.getCache().contains(Employee.class, 20));
        assertTrue("2lc entity cache is expected to contain Employee id = 30", emf.getCache().contains(Employee.class, 30));


        // evict entity 2lc
        emf.getCache().evictAll();

		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME+"Employee");
			
		try{	
			assertEquals("Expected no entities stored in the cache"+emp2LCStats, 0, emp2LCStats.getElementCountInMemory());
            assertFalse("2lc entity cache not expected to contain Employee id = 20", emf.getCache().contains(Employee.class, 20));
            assertFalse("2lc entity cache not expected to contain Employee id = 30", emf.getCache().contains(Employee.class, 30));
			// loading entity stored in previous session, we are expecting miss in 2lc
			Employee emp = getEmployee(em, 20);
			assertNotNull("Employee returned", emp);
			assertEquals("Expected 1 miss in 2LC"+generateEntityCacheStats(emp2LCStats), 1,  emp2LCStats.getMissCount());
			
		}	finally{
			em.close();
		}
	
		return "OK";	
	}


	/**
	 * Generate entity cache statistics for put, hit and miss count as one String
	 */
	public String generateEntityCacheStats(SecondLevelCacheStatistics stats){
		String result = "(hitCount="+stats.getHitCount()
				+", missCount="+stats.getMissCount()
				+", putCount="+stats.getPutCount()+").";
		
		return result;
	}
	
	
	public String getCacheRegionName(){

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
			throw new RuntimeException(	"transactional failure while persisting employee entity", e);
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
            throw new RuntimeException(	"transactional failure while persisting employee entity", e);
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

}
