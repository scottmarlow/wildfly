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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.StartException;

/**
 * CassandraInteraction is for interacting with Cassandra without static references to Cassandra classes.
 * TODO: switch to MethodHandle.invokeExact(...) for better performance
 *
 * @author Scott Marlow
 */
public class CassandraInteraction {

    private static final String COM_DATASTAX_DRIVER_CORE_CLUSTER = "com.datastax.driver.core.Cluster";
    private static final String COM_DATASTAX_DRIVER_CORE_CLUSTER_BUILDER = "com.datastax.driver.core.Cluster$Builder";
    private static final String COM_DATASTAX_DRIVER_CORE_SESSION ="com.datastax.driver.core.Session";

    private ConfigurationBuilder configurationBuilder;
    private Object clusterBuilder;

    CassandraInteraction(ConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
        // TODO: cache method handles + classes in constructor
    }

    // return com.datastax.driver.core.Cluster.Builder instance
    private Object getBuilder() throws StartException {
        try {
            if (clusterBuilder == null) {
                Class builderClass = getModuleClassLoader().loadClass(COM_DATASTAX_DRIVER_CORE_CLUSTER);
                // public static Cluster.Builder builder() {
                this.clusterBuilder = builderClass.getMethod("builder").invoke(null);
            }
            return clusterBuilder;
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (InvocationTargetException e) {
            throw new StartException(e);
        }
    }

    // call cluster = builder.build();
    // public Cluster build()
    protected Object build() throws StartException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            // returns com.datastax.driver.core.Cluster
            final Class clusterclass = getModuleClassLoader().loadClass(COM_DATASTAX_DRIVER_CORE_CLUSTER);
            final Class builderclass = getModuleClassLoader().loadClass(COM_DATASTAX_DRIVER_CORE_CLUSTER_BUILDER);
            MethodHandle build = lookup.findVirtual(builderclass, "build", MethodType.methodType(clusterclass));
            return build.invokeWithArguments(getBuilder());
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }

    // call session = cluster.connect(keySpace);
    protected Object connect(Object cluster, String keySpace) throws StartException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            final Class sessionClass = getModuleClassLoader().loadClass(COM_DATASTAX_DRIVER_CORE_SESSION);
            MethodHandle connect = lookup.findVirtual(cluster.getClass(), "connect", MethodType.methodType(sessionClass, String.class));
            connect = MethodHandles.insertArguments(connect, 0, cluster, keySpace);
            return connect.invoke();
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }

    // call withClusterName(String name)
    protected void withClusterName(String clusterName) throws StartException {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle withClusterName = lookup.findVirtual(getBuilder().getClass(), "withClusterName", MethodType.methodType(getBuilder().getClass(), String.class));
            withClusterName.invoke(getBuilder(), clusterName);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }

    // call builder.withPort(target.getPort());
    protected void withPort(int port) throws StartException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle withPort = lookup.findVirtual(getBuilder().getClass(), "withPort", MethodType.methodType(getBuilder().getClass(), int.class));
            withPort = MethodHandles.insertArguments(withPort, 0, getBuilder(), port);
            withPort.invoke();
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }

    protected void addContactPoint(String host) throws StartException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle withPort = lookup.findVirtual(getBuilder().getClass(), "addContactPoint", MethodType.methodType(getBuilder().getClass(), String.class));
            withPort = MethodHandles.insertArguments(withPort, 0, getBuilder(), host);
            withPort.invoke();
        } catch (NoSuchMethodException e) {
            throw new StartException(e);
        } catch (IllegalAccessException e) {
            throw new StartException(e);
        } catch (Throwable throwable) {
            throw new StartException(throwable);
        }
    }

    protected void clusterClose(Object cluster) throws Throwable {
        // com.datastax.driver.core.Cluster.close()
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle build = lookup.findVirtual(cluster.getClass(), "close", MethodType.methodType(void.class));
        build.invoke(cluster);
    }

    protected void sessionClose(Object session) throws Throwable {
        // com.datastax.driver.core.Session.close()
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle build = lookup.findVirtual(session.getClass(), "close", MethodType.methodType(void.class));
        build.invoke(session);
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
