/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * {@link SitemapSplitter} The splitted sitemap files are written to the filesystem.
 */
public class FileSystemSitemapSplitter extends FolderBasedSitemapSplitter {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemSitemapSplitter.class);
    private String sitemapDestinationFolder;

    /**
     * The SitemapSplitter will split the sitemap that is provided.
     *
     * @param urlset                   UrlSet - contains the set of urls which reflects the sitemap.
     * @param sitemapDestinationFolder The destination folder where the splitted sitemap files must be stored.
     */
    public FileSystemSitemapSplitter(final Urlset urlset, final String sitemapDestinationFolder) {
        super(urlset.getUrls());
        if (StringUtils.isEmpty(sitemapDestinationFolder)) {
            throw new IllegalArgumentException("Destination Folder Path needs to be defined and cannot be empty");
        }
        this.sitemapDestinationFolder = stripTailingSlashOffFolderPath(sitemapDestinationFolder);

        if (sitemapDestinationFolder == null) {
            LOG.error("Error while obtaining the sitemap folder from the filesystem. "
                    + "No sitemap index files will be created.");
            throw new IllegalStateException("Error while obtaing the sitemap folder from the filesystem.");
        }

        if (!prepareFileSystemSiteMapFolder(this.sitemapDestinationFolder)) {
            throw new IllegalArgumentException("Failed to prepare the destination folder on the file system");
        }
    }

    /**
     * Take care that the folder is correctly formatted.
     *
     * @param folderName the initial folder path
     * @return The correctly formatted foldername.
     */
    private static String stripTailingSlashOffFolderPath(final String folderName) {
        String formattedFolderName = folderName;

        if (folderName.endsWith("/") || folderName.endsWith("\\")) {
            formattedFolderName = folderName.substring(0, folderName.length() - 1);
        }
        return formattedFolderName;
    }

    /**
     * Prepare the sitemap folder on the filesystem, where the sitemap files will be written to.
     *
     * @param siteMapFolderPath the folder where to write.
     * @return true if the  sitemap folder has been prepared.
     */
    private static boolean prepareFileSystemSiteMapFolder(final String siteMapFolderPath) {
        // If the folder exists it will be deleted.
        // After deletion it will be recreated, so we are sure it is an empty folder.
        File siteMapFolder = new File(siteMapFolderPath);
        boolean exists = siteMapFolder.exists();
        if (exists) {
            boolean success = true;
            if (siteMapFolder.isDirectory()) {
                File[] files = siteMapFolder.listFiles();
                if (files != null) {
                    for (File fileToDelete : files) {
                        if (!fileToDelete.delete()) {
                            success = false;
                        }
                    }
                }
            }
            if (!success) {
                // Deletion failed
                throw new IllegalStateException("Cannot clean up sitemap folder.");
            }
        }

        return new File(siteMapFolderPath).mkdirs();
    }

    /**
     * Write the generated sitemap to an XML Document somewhere on a filesystem. It must be configured where the
     * location of the file will be stored.
     *
     * @param content  the xml content to write to the file
     * @param filename the path (including) file name to write the xml content to
     */
    protected void writeToFolder(final String content, final String filename) {
        BufferedWriter out = null;
        String path = sitemapDestinationFolder + File.separator + filename;
        try {
            out = new BufferedWriter(new FileWriter(path));
            out.write(content);
        } catch (IOException e) {
            LOG.error("Error occurred while writing the sitemap to a file", e);
        } finally {
            closeQuietly(out);
        }
    }

}
