/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.persistence.cdi;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceAgent;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@ApplicationScoped
public class PersistenceAgentTestCase {
    @Deployment
    public static WebArchive deployment() {
        String xml = "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"4.0\">"
                + "<persistence-unit name=\"agent\"><jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>"
                + "<properties><property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>"
                + "</persistence-unit></persistence>";
        return ShrinkWrap.create(WebArchive.class, "persistence-agent.war")
                .addClasses(Employee.class, PersistenceAgentBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(xml), "classes/META-INF/persistence.xml");
    }

    @PersistenceAgent(unitName = "agent")
    private EntityAgent agent;
    @PersistenceAgent(unitName = "agent", properties = @jakarta.persistence.PersistenceProperty(name = "hibernate.jdbc.batch_size", value = "5"))
    private EntityAgent configured;
    @PersistenceContext(unitName = "agent")
    private EntityManager manager;
    @Resource
    private UserTransaction transaction;
    @EJB
    private PersistenceAgentBean bean;

    @Test
    public void injectionCommitRollbackAndLifecycle() throws Exception {
        assertTrue(bean.checkInjection());
        assertTrue(agent.isOpen());
        assertThrows(IllegalStateException.class, agent::close);
        assertThrows(IllegalStateException.class, agent::getTransaction);
        assertThrows(jakarta.persistence.TransactionRequiredException.class, () -> agent.insert(new Employee()));
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream output = new java.io.ObjectOutputStream(bytes)) {
            output.writeObject(agent);
        }
        EntityAgent restored;
        try (java.io.ObjectInputStream input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (EntityAgent) input.readObject();
        }
        Class<?> statelessSession = Class.forName("org.hibernate.StatelessSession");
        Object committedDelegate;
        transaction.begin();
        try {
            committedDelegate = agent.unwrap(statelessSession);
            org.junit.Assert.assertSame(agent.unwrap(statelessSession), restored.unwrap(statelessSession));
            org.junit.Assert.assertNotSame(agent.unwrap(statelessSession), configured.unwrap(statelessSession));
            assertEquals("5", configured.getProperties().get("hibernate.jdbc.batch_size").toString());
            bean.insert(1);
            Employee employee = new Employee();
            employee.setId(2);
            employee.setName("CDI");
            agent.insert(employee);
            transaction.commit();
        } catch (Exception | AssertionError e) {
            transaction.rollback();
            throw e;
        }
        assertEquals(false, statelessSession.getMethod("isOpen").invoke(committedDelegate));
        assertTrue(bean.checkNonTxWrappers());
        Object rolledBackDelegate;
        transaction.begin();
        try {
            rolledBackDelegate = agent.unwrap(statelessSession);
            bean.insert(3);
        } finally {
            transaction.rollback();
        }
        assertEquals(false, statelessSession.getMethod("isOpen").invoke(rolledBackDelegate));
        transaction.begin();
        try {
            assertEquals(Long.valueOf(2), manager.createQuery("select count(e) from Employee e", Long.class)
                    .setQueryFlushMode(jakarta.persistence.QueryFlushMode.FLUSH).getSingleResult());
            assertEquals(1, manager.createStatement("update Employee e set e.name = :name where e.id = :id")
                    .setParameter("name", "updated", String.class).setParameter("id", 1, Integer.class).execute());
            manager.createNativeStatement("CREATE ALIAS IF NOT EXISTS AGENT_ABS FOR 'java.lang.Math.abs(int)'").execute();
            try {
                jakarta.persistence.StoredProcedureQuery procedure = manager.createStoredProcedureQuery("AGENT_ABS");
                jakarta.persistence.Parameter<Integer> parameter = procedure.registerParameter(1, Integer.class, jakarta.persistence.ParameterMode.IN);
                procedure.setParameter(parameter, -1);
                assertEquals(1, ((Number) procedure.getSingleResult()).intValue());
            } finally {
                manager.createNativeStatement("DROP ALIAS AGENT_ABS").execute();
            }
        } finally {
            transaction.rollback();
        }
    }
}
