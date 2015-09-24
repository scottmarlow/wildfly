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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
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

    private MongoClient client;
    private DB database;
    private MongoInteraction mongoInteraction;

    public MongoDriverService(ConfigurationBuilder builder) {
        this.configurationBuilder = builder;
        mongoInteraction = new MongoInteraction(configurationBuilder);
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        try {
            MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
            builder.description(configurationBuilder.getDescription());
            ArrayList<ServerAddress> serverAddressArrayList = new ArrayList<>();
            List<HostPortPair> targets = configurationBuilder.getTargets();
            for(HostPortPair target : targets) {
                if(target.getHost() != null && target.getPort() > 0) {
                    serverAddressArrayList.add(new ServerAddress(target.getHost(),target.getPort()));
                }
                else if(target.getHost() != null) {
                    serverAddressArrayList.add(new ServerAddress(target.getHost()));
                }
            }

            client = new MongoClient(serverAddressArrayList,builder.build());
            if(configurationBuilder.getDatabase() != null) {
                database = client.getDB(configurationBuilder.getDatabase());
            }

        } catch (UnknownHostException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        client.close();
        client = null;
    }

    @Override
    public MongoDriverService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public MongoClient getClient() {
        return client;
    }

    public DB getDatabase() {
        return database;
    }

}
