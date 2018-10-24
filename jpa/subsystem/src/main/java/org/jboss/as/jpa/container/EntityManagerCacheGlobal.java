/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2018, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.container;

import java.util.concurrent.ConcurrentLinkedDeque;

import javax.persistence.EntityManager;

import org.jipijapa.plugin.spi.EntityManagerCache;

/**
 * EntityManagerCacheGlobal is a Global (per application deployment/per persistence unit cache, that is not limited in size.
 *
 * @author Scott Marlow
 */
public class EntityManagerCacheGlobal implements EntityManagerCache {

    private final ConcurrentLinkedDeque<EntityManager> cache = new ConcurrentLinkedDeque<>();

    @Override
    public EntityManager get() {
        EntityManager result = null;
        do {
            if (cache.isEmpty()) {
                return null;
            }
            result = cache.pop();
        } while(!result.isOpen());
        return result;
    }

    @Override
    public void put(EntityManager entityManager) {
        cache.push(entityManager);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
