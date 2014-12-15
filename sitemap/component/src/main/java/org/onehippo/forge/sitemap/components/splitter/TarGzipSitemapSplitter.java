/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.splitter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.onehippo.forge.sitemap.components.model.Urlset;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * A site map splitter that creates a tar.gz archive with a folder structure that has the site map index xml and the
 * different sitemap.xml files in the index. The archive is written to the output stream specified in the constructor.
 */
public class TarGzipSitemapSplitter extends FolderBasedSitemapSplitter {

    private final OutputStream outputStream;
    private TarArchiveOutputStream output = null;
    private long bytesWritten = 0;

    /**
     * Creates a splitter that will split the passed {@link Urlset} into an archive that is written to the passed
     * {@link OutputStream}.
     *
     * @param urlSet the url set (site map) to split
     * @param outputStream the output stream to write the archive to
     */
    public TarGzipSitemapSplitter(Urlset urlSet, OutputStream outputStream) {
        super(urlSet.getUrls());
        this.outputStream = outputStream;
    }

    @Override
    /**
     * Writes the specified file to the tar archive
     */
    protected void writeToFolder(final String content, final String filename) {
        TarArchiveEntry entry = new TarArchiveEntry(filename);
        entry.setSize(content.length());
        try {
            output.putArchiveEntry(entry);
            output.write(content.getBytes());
            output.closeArchiveEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot add file to archive", e);
        }
    }

    @Override
    /**
     * Sets up the archive / compression stream and then calls super.split(). After it cleans up all the streams that
     * were created.
     */
    public boolean split() {
        GzipCompressorOutputStream gzipCompressorOutputStream = null;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(outputStream);
            gzipCompressorOutputStream = new GzipCompressorOutputStream(bos);
            output = new TarArchiveOutputStream(gzipCompressorOutputStream);
            boolean wasSplitted = super.split();
            if (wasSplitted) {
                output.finish();
                bytesWritten = output.getBytesWritten();
            }
            return wasSplitted;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write archive", e);
        } finally {
            closeQuietly(output);
            closeQuietly(gzipCompressorOutputStream);
            closeQuietly(bos);
        }
    }

    public long getBytesWritten() {
        return bytesWritten;
    }
}