/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.xpc.bean;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;

/**
 * Event
 *
 * @author Scott Marlow
 */
@Embeddable
// @Cacheable(true) // allow second level cache to cache Employee
public class EventId implements Serializable {

    public String id;
    public long empPK;

            public EventId(String id) {
                    this();
                    setId(id);
            }

             public EventId(String id, long empPK) {
                        this();
                        setId(id);
                        setEmpPK(empPK);
                }

            public String getId() {
                    return id;
            }

            public void setId(String id) {
                    this.id = id;
                    empPK = 100;
            }

          public long getEmpPK() {
              return empPK;
          }

          public void setEmpPK(long emp) {
              this.empPK = emp;
          }

            public EventId() {
            }

            public boolean equals(Object o) {
                    if (o instanceof EventId) {
                            return id.equals(EventId.class.cast(o).id);
                    } else {
                            return false;
                    }
            }

            public int hashCode() {
                    return id.hashCode();
            }

            public String toString() {
                    return id.toString();
            }
    }

