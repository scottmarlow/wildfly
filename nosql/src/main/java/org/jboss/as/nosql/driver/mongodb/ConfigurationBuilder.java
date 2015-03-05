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

/**
 * ConfigurationBuilder
 *
 * @author Scott Marlow
 */
public class ConfigurationBuilder {

    private ArrayList<HostPortPair> targets = new ArrayList<>();

    private String JNDIName;
    private String database;
    private String description;
    private String moduleName = // name of MongoDB module
            "org.mongodb.driver";

    public ConfigurationBuilder addTarget(String hostname) throws UnknownHostException {
        targets.add(new HostPortPair(hostname));
        return this;
    }

    public ConfigurationBuilder addTarget(String hostname, int port) throws UnknownHostException {
        targets.add(new HostPortPair(hostname, port));
        return this;
    }

    public ConfigurationBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public List<HostPortPair> getTargets() {
            return targets;
        }

    public void setJNDIName(String JNDIName) {
        this.JNDIName = JNDIName;
    }

    public String getJNDIName() {
        return JNDIName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
