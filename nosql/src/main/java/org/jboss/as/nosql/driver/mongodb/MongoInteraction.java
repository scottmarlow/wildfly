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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.StartException;

/**
 * MongoInteraction
 *
 * @author Scott Marlow
 *
 */
public class MongoInteraction {

    private ArrayList serverAddressArrayList = new ArrayList(); // List<ServerAddress>
    private ConfigurationBuilder configurationBuilder;

    private final String COM_MONGODB_CLIENT = "com.mongodb.MongoClient";
    private final String COM_MONGODB_CLIENTOPTIONS = "com.mongodb.MongoClientOptions";
    private final String COM_MONGODB_CLIENTOPTIONS_BUILDER = "com.mongodb.MongoClientOptions$Builder";
    private final String COM_MONGODB_SERVERADDRESS = "com.mongodb.ServerAddress";
    private final String COM_MONGODB_DB = "com.mongodb.DB";

    public MongoInteraction(ConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
    }

    public void hostPort(String host, int port) throws StartException {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class serverAddressClass = getModuleClassLoader().loadClass(COM_MONGODB_SERVERADDRESS);

            if( port > 0) {
                MethodHandle serverAddressCtor = lookup.findConstructor(serverAddressClass, MethodType.methodType(void.class, String.class, int.class));
                serverAddressArrayList.add(serverAddressCtor.invoke(host, port));
            }
            else {
                MethodHandle serverAddressCtor = lookup.findConstructor(serverAddressClass, MethodType.methodType(void.class, String.class));
                serverAddressArrayList.add(serverAddressCtor.invoke(host));
            }
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        } catch (StartException e) {
            throw new StartException(e);
        } catch (UnknownHostException e) {
            throw new StartException(e);
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }

    }

    public Object mongoClient() throws StartException {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class mongoClientOptionsBuilderClass = getModuleClassLoader().loadClass(COM_MONGODB_CLIENTOPTIONS_BUILDER);

            final MethodHandle mongoClientOptionsBuilderCtor = lookup.findConstructor(mongoClientOptionsBuilderClass, MethodType.methodType(void.class));
            // MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
            final Object builder = mongoClientOptionsBuilderCtor.invoke();

            // MongoClientOptions.Builder description(String description) {
            final Class mongoClientOptionsClass = getModuleClassLoader().loadClass(COM_MONGODB_CLIENTOPTIONS);
            final MethodHandle description = lookup.findVirtual(mongoClientOptionsBuilderClass,"description", MethodType.methodType(mongoClientOptionsBuilderClass, String.class));
            // builder.description(configurationBuilder.getDescription());
            description.invoke(builder, configurationBuilder.getDescription());

            // MongoClientOptions mongoClientOptions = builder.build();
            final MethodHandle build = lookup.findVirtual(mongoClientOptionsBuilderClass, "build", MethodType.methodType(mongoClientOptionsClass));
            final Object mongoClientOptions = build.invoke(builder);

            // return new MongoClient(serverAddressArrayList,mongoClientOptions);
            // public MongoClient(List<ServerAddress> seeds, MongoClientOptions options) {
            final Class mongoClientClass = getModuleClassLoader().loadClass(COM_MONGODB_CLIENT);
            MethodHandle mongoClientCtor = lookup.findConstructor(mongoClientClass, MethodType.methodType(void.class, List.class, mongoClientOptionsClass));
            return mongoClientCtor.invoke(serverAddressArrayList, mongoClientOptions);

        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        } catch (StartException e) {
            throw new StartException(e);
        } catch (UnknownHostException e) {
            throw new StartException(e);
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }

    }

    public Object getDB(Object client) throws StartException {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class mongoClientClass = getModuleClassLoader().loadClass(COM_MONGODB_CLIENT);
            final Class dbClass = getModuleClassLoader().loadClass(COM_MONGODB_DB);
            MethodHandle mongoClient = lookup.findVirtual(mongoClientClass, "getDB", MethodType.methodType(dbClass, String.class));
            return mongoClient.invoke(client, configurationBuilder.getDatabase());
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        } catch (StartException e) {
            throw new StartException(e);
        } catch (UnknownHostException e) {
            throw new StartException(e);
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }


    public void close(Object client) throws Throwable {
        if( client != null) {
            // void close()
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class mongoClientClass = getModuleClassLoader().loadClass(COM_MONGODB_CLIENT);
            MethodHandle mongoClose = lookup.findVirtual(mongoClientClass, "close", MethodType.methodType(void.class));
            mongoClose.invoke(client);
        }
    }

    protected ClassLoader getModuleClassLoader() throws StartException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        try {
            Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(configurationBuilder.getModuleName()));
            return module.getClassLoader();
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }
    }

}
