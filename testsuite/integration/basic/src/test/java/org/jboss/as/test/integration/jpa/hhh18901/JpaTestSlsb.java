/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hhh18901;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.metamodel.EntityType;
import org.junit.Assert;

@Stateless
public class JpaTestSlsb {

    @PersistenceUnit(unitName = "mainNonTXPu")
    private EntityManagerFactory emf;

    @PersistenceContext(unitName = "mainPu")
    private EntityManager em;

    public void testMainArchiveEntity() {
        final EntityType<MainArchiveEntity> meta = emf.getMetamodel().entity(MainArchiveEntity.class);
        Assert.assertNotNull("class must be an entity", meta);
        MainArchiveEntity entity = new MainArchiveEntity();
        entity.setId(1);
        entity.setName("Bob");
        entity.setAddress("123 Fake St");
        em.persist(entity);
        em.flush();
    }

    public void testJarFileEntity() {
        final EntityType<PartTimeEmployee> meta = emf.getMetamodel().entity(PartTimeEmployee.class);
        Assert.assertNotNull("class must be an entity", meta);
        PartTimeEmployee entity = new PartTimeEmployee();
        entity.setId(1);
        entity.setFirstName("Bob");
        // entity.set("123 Fake St");
        em.persist(entity);
        em.flush();
    }
}
