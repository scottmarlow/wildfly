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

import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * CassandraDriverService represents the connection into Cassandra
 *
 * @author Scott Marlow
 */
public class CassandraDriverService implements Service<CassandraDriverService> {

    private final ConfigurationBuilder configurationBuilder;
    private final CassandraInteraction cassandraInteraction;
    private Object cluster;  // represents connection into Cassandra
    private Object session;  // only set if keyspaceName is specified


    public CassandraDriverService(ConfigurationBuilder builder) {
        configurationBuilder = builder;
        cassandraInteraction = new CassandraInteraction(configurationBuilder);
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        Object builder = cassandraInteraction.getBuilder();

        List<HostPortPair> targets = configurationBuilder.getTargets();

        for(HostPortPair target : targets) {
            if(target.getPort() > 0) {
                cassandraInteraction.withPort(builder, target.getPort());
            }
            if(target.getHost() != null) {
                cassandraInteraction.addContactPoint(builder, target.getHost());
            }
        }

        if(configurationBuilder.getDescription() != null) {
            cassandraInteraction.withClusterName(builder, configurationBuilder.getDescription());
        }
        cluster = cassandraInteraction.build(builder);

        String keySpace = configurationBuilder.getKeySpace();
        if(keySpace != null) {
            session = cassandraInteraction.connect(cluster, keySpace);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        try {
            if (session != null) {
                cassandraInteraction.sessionClose(session);
                session = null;
            }
            cassandraInteraction.clusterClose(cluster);
            cluster = null;
        } catch( Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public CassandraDriverService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Object getCluster() {
        return cluster;
    }

    public Object getSession() {
        return session;
    }


}
