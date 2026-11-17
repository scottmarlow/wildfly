/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.persistence.cdi;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.PersistenceAgent;
import javax.naming.InitialContext;

@Stateless
@PersistenceAgent(name = "agents/named", unitName = "agent")
@PersistenceAgent(name = "agents/second", unitName = "agent")
public class PersistenceAgentBean {
    @PersistenceAgent(unitName = "agent")
    private EntityAgent field;
    private EntityAgent setter;
    @jakarta.persistence.PersistenceContext(unitName = "agent")
    private jakarta.persistence.EntityManager manager;

    @PersistenceAgent(unitName = "agent")
    public void setAgent(EntityAgent agent) {
        setter = agent;
    }

    public void insert(int id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("agent-" + id);
        field.insert(employee);
    }

    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED)
    public boolean checkNonTxWrappers() {
        Employee employee = manager.get(Employee.class, 1);
        if (manager.contains(employee)) { return false; }
        employee = manager.getMultiple(Employee.class, java.util.List.of(1)).get(0);
        if (manager.contains(employee)) { return false; }
        employee = manager.findMultiple(Employee.class, java.util.List.of(1)).get(0);
        if (manager.contains(employee)) { return false; }
        employee = (Employee) manager.createQuery("from Employee e where e.id = 1").getSingleResult();
        if (manager.contains(employee)) { return false; }
        employee = manager.createQuery("from Employee e where e.id = 1").ofType(Employee.class).getSingleResult();
        if (manager.contains(employee)) { return false; }
        employee = manager.createQuery("from Employee e where e.id = 1").withEntityGraph(manager.createEntityGraph(Employee.class)).getSingleResult();
        if (manager.contains(employee)) { return false; }
        employee = manager.createQuery("from Employee e where e.id = 1", manager.createEntityGraph(Employee.class)).getSingleResult();
        return !manager.contains(employee);
    }

    public boolean checkInjection() throws Exception {
        EntityAgent named = (EntityAgent) new InitialContext().lookup("java:comp/env/agents/named");
        EntityAgent second = (EntityAgent) new InitialContext().lookup("java:comp/env/agents/second");
        return field != null && setter != null && named != null && second != null;
    }
}
