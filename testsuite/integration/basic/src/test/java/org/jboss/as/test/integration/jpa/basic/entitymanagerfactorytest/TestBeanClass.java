package org.jboss.as.test.integration.jpa.basic.entitymanagerfactorytest;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManagerFactory;


/**
 * TestBeanClass
 *
 * @author Scott Marlow
 */
@RequestScoped
public class TestBeanClass {
    private EntityManagerFactory emf;

    @PostConstruct
    public void postConstruct() throws NamingException {
        InitialContext ic = new InitialContext();
        emf = (EntityManagerFactory) ic.lookup("java:/sampleEMF");

    }

    public EntityManagerFactory entityManagerFactory() {
        return emf;
    }

}
