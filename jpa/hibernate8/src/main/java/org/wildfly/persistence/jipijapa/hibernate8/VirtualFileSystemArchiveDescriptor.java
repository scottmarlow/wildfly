/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate8;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.jboss.vfs.VirtualFile;


/**
 * Representation of an archive in the JBoss VirtualFileSystem API in terms of how to walk entries.
 *
 * <p>Rewritten for the Hibernate ORM 8 archive SPI, which replaced the {@code visitArchive(ArchiveContext)}
 * callback with {@link #visitClassEntries(Consumer)} (class entries only) plus explicit
 * {@link #findEntry(String)} / {@link #resolveJarFileReference(String)} lookups.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptor implements ArchiveDescriptor {
    private final ArchiveDescriptorFactory archiveDescriptorFactory;
    private final URL url;
    private final VirtualFile root;

    public VirtualFileSystemArchiveDescriptor(ArchiveDescriptorFactory archiveDescriptorFactory, URL url, VirtualFile archiveRoot, String entryBase) {
        this.archiveDescriptorFactory = archiveDescriptorFactory;
        this.url = url;
        if ( entryBase != null && entryBase.length() > 0 && ! "/".equals( entryBase ) ) {
            this.root = archiveRoot.getChild( entryBase );
        }
        else {
            this.root = archiveRoot;
        }
    }

    public VirtualFile getRoot() {
        return root;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
        processVirtualFile( root, null, entryConsumer );
    }

    private void processVirtualFile(VirtualFile virtualFile, String path, Consumer<ArchiveEntry> entryConsumer) {
        if ( path == null ) {
            path = "";
        }
        else {
            if ( !path.endsWith( "/" ) ) {
                path = path + "/";
            }
        }

        for ( VirtualFile child : virtualFile.getChildren() ) {
            if ( !child.exists() ) {
                // should never happen conceptually, but...
                continue;
            }

            if ( child.isDirectory() ) {
                processVirtualFile( child, path + child.getName(), entryConsumer );
                continue;
            }

            // the ORM 8 scanner only indexes class entries; mapping files are resolved via findEntry
            if ( !child.getName().endsWith( ".class" ) ) {
                continue;
            }

            entryConsumer.accept( toArchiveEntry( child, path + child.getName() ) );
        }
    }

    /**
     * Example call from the Hibernate scanner that would be used to look up orm.xml in the
     * persistence unit root (same path as persistence.xml).
     *
     * Example usage:
     *     final ArchiveEntry entry = archiveDescriptor.findEntry( "META-INF/orm.xml" );
     *
     * @param path relative path within the archive, e.g. {@code META-INF/orm.xml}
     * @return the matching entry, or {@code null} if not present
     */
    @Override
    public ArchiveEntry findEntry(String path) {
        final VirtualFile child = root.getChild( path );
        if ( child == null || !child.exists() || child.isDirectory() ) {
            return null;
        }
        return toArchiveEntry( child, path );
    }

    @Override
    public ArchiveDescriptor resolveJarFileReference(String jarFileReference) {
        // try it as a relative reference within this archive first
        final ArchiveEntry entry = findEntry( jarFileReference );
        if ( entry != null ) {
            try {
                return archiveDescriptorFactory.buildArchiveDescriptor( entry.getUri().toURL() );
            }
            catch (MalformedURLException e) {
                throw new ArchiveException( "Unable to convert relative <jar-file/> reference to URL [" + jarFileReference + "]", e );
            }
        }

        // otherwise resolve it relative to this archive's parent (sibling jar-file references)
        final VirtualFile parent = root.getParent();
        if ( parent != null ) {
            final VirtualFile sibling = parent.getChild( jarFileReference );
            if ( sibling != null && sibling.exists() ) {
                try {
                    return archiveDescriptorFactory.buildArchiveDescriptor( sibling.toURL() );
                }
                catch (MalformedURLException e) {
                    throw new ArchiveException( "Unable to convert <jar-file/> reference to URL [" + jarFileReference + "]", e );
                }
            }
        }

        throw new ArchiveException( "Unable to resolve <jar-file/> reference - " + jarFileReference );
    }

    private ArchiveEntry toArchiveEntry(VirtualFile file, String relativeName) {
        final String name = file.getPathName();
        final URI uri;
        try {
            uri = file.toURI();
        }
        catch (URISyntaxException e) {
            throw new ArchiveException( "Unable to determine URI for archive entry [" + name + "]", e );
        }
        final InputStreamAccess inputStreamAccess = new VirtualFileInputStreamAccess( name, file );

        return new ArchiveEntry() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getNameWithinArchive() {
                return relativeName;
            }

            @Override
            public URI getUri() {
                return uri;
            }

            @Override
            public InputStreamAccess getStreamAccess() {
                return inputStreamAccess;
            }
        };
    }
}
