/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.nosql.driver.neo4j;

import static org.wildfly.nosql.common.NoSQLLogger.ROOT_LOGGER;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.neo4j.driver.v1.Driver;
import org.wildfly.nosql.common.spi.NoSQLConnection;

/**
 * Neo4jClientConnectionService represents the connection into Neo4J
 *
 * @author Scott Marlow
 */
public class Neo4jClientConnectionService implements Service<Neo4jClientConnectionService>, NoSQLConnection {

    private final ConfigurationBuilder configurationBuilder;
    // standard application server way to obtain target hostname + port for target NoSQL database server(s)
    private Map<String, OutboundSocketBinding> outboundSocketBindings = new HashMap<String, OutboundSocketBinding>();
    private final Neo4jInteraction neo4jInteraction;
    private Driver driver;  // Driver is thread safe but Session is not

    public Neo4jClientConnectionService(ConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
        neo4jInteraction = new Neo4jInteraction();
    }

    public Injector<OutboundSocketBinding> getOutboundSocketBindingInjector(String name) {
        return new MapInjector<String, OutboundSocketBinding>(outboundSocketBindings, name);
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        for (OutboundSocketBinding target : outboundSocketBindings.values()) {
            if (target.getUnresolvedDestinationAddress() != null) {
                neo4jInteraction.addContactPoint(target.getUnresolvedDestinationAddress());
            }
            if (target.getDestinationPort() > 0) {
                neo4jInteraction.withPort(target.getDestinationPort());
            }

        }

        //if (configurationBuilder.getDescription() != null) {
            // neo4jInteraction.withClusterName(configurationBuilder.getDescription());
        // }
        driver = neo4jInteraction.build();

    }

    @Override
    public void stop(StopContext stopContext) {
        try {
            neo4jInteraction.driverClose(driver);
            driver = null;
        } catch (Throwable throwable) {
            ROOT_LOGGER.driverFailedToStop(throwable);
        }
    }

    @Override
    public Neo4jClientConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Driver getDriver() {
        return driver;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if ( Driver.class.isAssignableFrom( clazz ) ) {
            return (T) driver;
        }
        //if ( Session.class.isAssignableFrom( clazz)) {
        //    return (T) session;
        //}
        throw ROOT_LOGGER.unassignable(clazz);
    }

}
