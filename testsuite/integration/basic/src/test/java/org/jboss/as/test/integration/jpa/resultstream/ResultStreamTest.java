/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.resultstream;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ejb.Stateful;
import jakarta.persistence.PersistenceContext;

import jakarta.persistence.EntityManager;

/**
 * Stateful session bean for testing Jakarta Persistence 2.2 API
 * jakarta.persistence.Query#getResultStream
 *
 * @author Gail Badner
 */
@Stateful
public class ResultStreamTest {

    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public Ticket createTicket() throws Exception {
        Ticket t = new Ticket();
        t.setNumber("111");

        em.persist(t);

        return t;
    }

    public List<Ticket> getTicketsOrderedById() {
        // Consume and close the JDBC-backed stream before the invocation's transaction ends.
        try (Stream<Ticket> stream = em.createQuery("select t from Ticket t order by t.id", Ticket.class).getResultStream()) {
            return stream.collect(Collectors.toList());
        }
    }

    public void deleteTickets() {
        em.createQuery( "delete from Ticket" ).executeUpdate();
    }
}
