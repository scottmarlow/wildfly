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

package org.jboss.as.nosql.driver.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * CassandraDriverService
 *
 * @author Scott Marlow
 */
public class CassandraDriverService implements Service<CassandraDriverService> {
    // TODO: allow set of host/port pairs to be specified
    private final String profileName, hostName, keyspaceName;
    private final int port;

    private Cluster cluster;
    private Session session;  // set if keyspaceName is specified

    public CassandraDriverService(String profileName, String hostName, int port, String keyspaceName) {
        this.hostName = hostName;
        this.profileName = profileName;
        this.port = port;
        this.keyspaceName = keyspaceName;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addTarget(hostName, port);
        builder.setUniqueName(profileName);
        cluster = builder.build();
        if(keyspaceName != null) {
            session = cluster.connect(keyspaceName);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        if(session != null) {
            session.close();
            session = null;
        }
        cluster.close();
        cluster = null;
    }

    @Override
    public CassandraDriverService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Session getSession() {
        return session;
    }

}
