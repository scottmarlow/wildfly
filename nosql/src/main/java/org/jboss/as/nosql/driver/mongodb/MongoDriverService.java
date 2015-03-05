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

package org.jboss.as.nosql.driver.mongodb;

import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * MongoDriverService
 *
 * @author Scott Marlow
 */
public class MongoDriverService implements Service<MongoDriverService> {
    final ConfigurationBuilder configurationBuilder;

    private Object client;
    private Object database;
    private MongoInteraction mongoInteraction;

    public MongoDriverService(ConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
        mongoInteraction = new MongoInteraction(configurationBuilder);
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        List<HostPortPair> targets = configurationBuilder.getTargets();
        for(HostPortPair target : targets) {
            // serverAddressArrayList.add(new ServerAddress(target.getHost(),target.getPort()));
            mongoInteraction.hostPort(target.getHost(),target.getPort());
        }
        client = mongoInteraction.mongoClient();
        if(configurationBuilder.getDatabase() != null) {
            database = mongoInteraction.getDB(client);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        try {
            mongoInteraction.close(client);
        } catch (Throwable throwable) {
            throwable.printStackTrace(); // todo: logger
        }
        client = null;
    }

    @Override
    public MongoDriverService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Object getClient() {
        return client;
    }

    public Object getDatabase() {
        return database;
    }

}
