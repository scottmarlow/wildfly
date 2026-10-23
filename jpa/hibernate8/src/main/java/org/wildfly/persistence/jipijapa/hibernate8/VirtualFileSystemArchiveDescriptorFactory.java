/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate8;

import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.jboss.vfs.VFS;


/**
 * In Hibernate terms, the ArchiveDescriptorFactory contract is used to plug in handling for how to deal
 * with archives in various systems.  For JBoss, that means its VirtualFileSystem API.
 *
 * <p>Under the Hibernate ORM 8 scanning SPI this factory is registered via
 * {@code AvailableSettings.SCANNER_ARCHIVE_INTERPRETER} so the auto-discovered jandex scanning provider
 * builds its {@code ArchiveDescriptor}s against the JBoss VFS. Hibernate instantiates it reflectively,
 * hence the public no-arg constructor.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptorFactory extends StandardArchiveDescriptorFactory {
    static final VirtualFileSystemArchiveDescriptorFactory INSTANCE = new VirtualFileSystemArchiveDescriptorFactory();

    public VirtualFileSystemArchiveDescriptorFactory() {
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url) {
        return buildArchiveDescriptor( url, "" );
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url, String entryBase) {
        try {
            return new VirtualFileSystemArchiveDescriptor( this, url, VFS.getChild( url.toURI() ), entryBase );
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException( e );
        }
    }
}
