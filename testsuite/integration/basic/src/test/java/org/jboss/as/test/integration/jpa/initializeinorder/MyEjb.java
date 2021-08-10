package org.jboss.as.test.integration.jpa.initializeinorder;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;


/**
 * @author Scott Marlow
 */
@Singleton
@Startup
public class MyEjb {

    @PersistenceUnit(unitName = "pu1")
    EntityManagerFactory emf;

    @PersistenceUnit(unitName = "pu2")
    EntityManagerFactory emf2;

    @PersistenceContext(unitName = "pu1")
    EntityManager em;

    @PersistenceContext(unitName = "pu2")
    EntityManager em2;

    @PostConstruct
    public void postConstruct() {
        emf.isOpen();
        emf2.isOpen();
        em.isOpen();
        em.isOpen();

        TestState.addInitOrder(MyEjb.class.getSimpleName());
    }

    public boolean hasPersistenceContext() {
        return em != null && em2 != null;
    }
}
