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

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;

/**
 * ConfigurationBuilder
 *
 * @author Scott Marlow
 */
public class ConfigurationBuilder {

    private List<ServerAddress> target = new ArrayList<ServerAddress>();
    private MongoClientOptions.Builder builder = new MongoClientOptions.Builder();

    public ConfigurationBuilder addTarget(String hostname) throws UnknownHostException {
        target.add(new ServerAddress(hostname));
        return this;
    }

    public ConfigurationBuilder addTarget(String hostname, int port) throws UnknownHostException {
        target.add(new ServerAddress(hostname, port));
        return this;
    }

    public ConfigurationBuilder setDescription(String description) {
        builder.description(description);
        return this;
    }

    public MongoClient build() throws UnknownHostException {

        if (target != null)
            return new MongoClient(target, builder.build());
        else return new MongoClient();
    }
}
