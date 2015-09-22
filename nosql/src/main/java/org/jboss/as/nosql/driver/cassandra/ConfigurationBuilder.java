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

import java.util.ArrayList;
import java.util.List;

/**
 * ConfigurationBuilder
 *
 * @author Scott Marlow
 */
public class ConfigurationBuilder {
    private ArrayList<HostPortPair> targets = new ArrayList<>();
    private String description; //
    private String JNDIName;    // required global jndi name
    private String keyspace;    // optional Cassandra keyspace

    private String moduleName = // name of Cassandra module
            "com.datastax.cassandra.driver-core";

    public ConfigurationBuilder setPort(int port)  {
        HostPortPair pair = new HostPortPair(port);
        targets.add(pair);
        return this;
    }

    public ConfigurationBuilder addTarget(String hostname, int port)  {
        HostPortPair pair = new HostPortPair(hostname, port);
        targets.add(pair);
        return this;
    }

    public ConfigurationBuilder addTarget(String hostname)  {
        HostPortPair pair = new HostPortPair(hostname);
        targets.add(pair);
        return this;
    }

    public List<HostPortPair> getTargets() {
        return targets;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setJNDIName(String JNDIName) {
        this.JNDIName = JNDIName;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getJNDIName() {
        return JNDIName;
    }

    public String getDescription() {
        return description;
    }

    public String getKeySpace() {
        return keyspace;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }

}
