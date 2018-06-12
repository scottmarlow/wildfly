/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_10;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_11;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_12;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_13;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_20;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_21;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_22;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_23;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_24;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_25;
import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_26;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.persistence.Store;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.TaskContextImpl;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.KeyValuePair;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;

/**
 * Simple implementation of Infinispan {@link AdvancedLoadWriteStore} configured with a started container-managed {@link RemoteCacheContainer}
 * instance. Does not perform wrapping entries in Infinispan internal objects, this stores "raw" values.
 *
 * @author Radoslav Husar
 */
@Store(shared = true)
public class HotRodStore<K, V> implements AdvancedLoadWriteStore<K, V> {

    private InitializationContext ctx;

    private RemoteCache<byte[], byte[]> remoteCache;

    @Override
    public void init(InitializationContext ctx) {
        this.ctx = ctx;

        HotRodStoreConfiguration configuration = ctx.getConfiguration();
        RemoteCacheContainer remoteCacheContainer = configuration.attributes().attribute(HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER).get();
        String cacheConfiguration = configuration.attributes().attribute(HotRodStoreConfiguration.CACHE_CONFIGURATION).get();
        String cacheName = ctx.getCache().getName();

        try {
            ProtocolVersion protocolVersion = remoteCacheContainer.getConfiguration().version();

            // Currently enumerates protocols which do *not* yet support administration calls
            // TODO update the upstream API to be able to use comparison reliably; ordering is changed in 9.3
            if (EnumSet.of(PROTOCOL_VERSION_26, PROTOCOL_VERSION_25, PROTOCOL_VERSION_24, PROTOCOL_VERSION_23, PROTOCOL_VERSION_22, PROTOCOL_VERSION_21, PROTOCOL_VERSION_20, PROTOCOL_VERSION_13, PROTOCOL_VERSION_12, PROTOCOL_VERSION_11, PROTOCOL_VERSION_10).contains(protocolVersion)) {
                InfinispanLogger.ROOT_LOGGER.remoteCacheMustBeDefined(protocolVersion.toString(), cacheName);
                this.remoteCache = remoteCacheContainer.getCache(cacheName, false);
            } else {
                InfinispanLogger.ROOT_LOGGER.remoteCacheCreated(cacheName, cacheConfiguration);
                this.remoteCache = remoteCacheContainer.administration().getOrCreateCache(cacheName, cacheConfiguration);
            }
        } catch (HotRodClientException ex) {
            throw new PersistenceException(ex);
        }

    }

    @Override
    public void start() {
        // Do nothing -- remoteCacheContainer is already started
    }

    @Override
    public void stop() {
        // Do nothing -- remoteCacheContainer lifecycle is controlled by the application server
    }

    @SuppressWarnings("unchecked")
    @Override
    public MarshalledEntry<K, V> load(Object key) throws PersistenceException {
        byte[] bytes = this.remoteCache.get(this.marshall(key));
        if (bytes == null) {
            return null;
        }
        KeyValuePair<ByteBuffer, ByteBuffer> keyValuePair = (KeyValuePair<ByteBuffer, ByteBuffer>) this.unmarshall(bytes);
        return this.ctx.getMarshalledEntryFactory().newMarshalledEntry(key, keyValuePair.getKey(), keyValuePair.getValue());
    }

    @Override
    public void write(MarshalledEntry<? extends K, ? extends V> entry) {
        this.remoteCache.put(this.marshall(entry.getKey()), this.marshall(entry));
    }

    @Override
    public void writeBatch(Iterable<MarshalledEntry<? extends K, ? extends V>> marshalledEntries) {
        Map<byte[], byte[]> batch = new HashMap<>();
        for (MarshalledEntry<? extends K, ? extends V> entry : marshalledEntries) {
            batch.put(this.marshall(entry.getKey()), this.marshall(entry));
        }

        if (!batch.isEmpty()) {
            this.remoteCache.putAll(batch);
        }
    }

    @Override
    public boolean contains(Object key) {
        return this.remoteCache.containsKey(this.marshall(key));
    }

    @Override
    public boolean delete(Object key) {
        return this.remoteCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(this.marshall(key)) != null;
    }

    @Override
    public void process(KeyFilter<? super K> filter, CacheLoaderTask<K, V> task, Executor executor, boolean fetchValue, boolean fetchMetadata) {
        TaskContext taskContext = new TaskContextImpl();
        for (byte[] key : this.remoteCache.keySet()) {
            if (taskContext.isStopped()) {
                break;
            }
            @SuppressWarnings("unchecked") K typedKey = (K) this.unmarshall(key);
            if (filter == null || filter.accept(typedKey)) {
                try {
                    MarshalledEntry<K, V> marshalledEntry = this.load(key);
                    if (marshalledEntry != null) {
                        task.processEntry(marshalledEntry, taskContext);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public int size() {
        return this.remoteCache.size();
    }

    @Override
    public void clear() {
        this.remoteCache.clear();
    }

    @Override
    public void purge(Executor threadPool, PurgeListener<? super K> listener) {
        // Ignore
    }

    private byte[] marshall(Object key) throws PersistenceException {
        try {
            return (key instanceof WrappedByteArray) ? ((WrappedByteArray) key).getBytes() : this.ctx.getMarshaller().objectToByteBuffer(key);
        } catch (IOException | InterruptedException e) {
            throw new PersistenceException(e);
        }
    }

    private byte[] marshall(MarshalledEntry<?, ?> entry) {
        return this.marshall(new KeyValuePair<>(entry.getValueBytes(), entry.getMetadataBytes()));
    }

    private Object unmarshall(byte[] bytes) throws PersistenceException {
        try {
            return this.ctx.getMarshaller().objectFromByteBuffer(bytes);
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }

}
