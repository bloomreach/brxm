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

import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Date;

/**
 * {@link SitemapSplitter}
 * The splitted sitemap files are written to the repository.
 */
public class RepositorySitemapSplitter extends SitemapSplitter {
    private static final Logger LOG = LoggerFactory.getLogger(RepositorySitemapSplitter.class);
    private String sitemapRepositoryDestinationFolderName;
    private Session session;
    private static final String SITEMAP_FILENAME = "sitemap-index-";
    private static final String ASSETS_ROOT = "/content/assets";
    private static final String ASSETS_SITEMAP_ROOT = "sitemap";
    private Node assetsSiteMapRootFolder;
    /**
     * The SitemapSplitter will split the sitemap that is provided.
     *
     * @param urlset  UrlSet - contains the set of urls which reflects the sitemap.
     * @param session The {@link javax.jcr.Session} to use for writing
     * @param sitemapRositoryDestinationFolderName
     *                The name of the destination folder where the splitted sitemap files must be stored.
     */
    public RepositorySitemapSplitter(final Urlset urlset, final Session session,
                                     final String sitemapRositoryDestinationFolderName) {
        super(urlset.getUrls());
        if (StringUtils.isEmpty(sitemapRositoryDestinationFolderName)) {
            throw new IllegalArgumentException("Destination Folder Name needs to be defined and cannot be empty");
        }
        this.sitemapRepositoryDestinationFolderName = sitemapRositoryDestinationFolderName;
        this.session = session;
        this.assetsSiteMapRootFolder = obtainAssetSiteMapFolder();
    }

    @Override
    public void writeSiteMapFilesToDestination() {
        // We must write the sub sitemaps to different files.
        // A sitemap index file must be created via a separate process and will not be created
        // at this moment.
        if (assetsSiteMapRootFolder != null) {
            assetsSiteMapRootFolder = obtainDestinationSiteMapFolder(sitemapRepositoryDestinationFolderName);
            if (assetsSiteMapRootFolder == null) {
                LOG.error("Error while obtaining the sitemap folder from the repository. "
                        + "No assets will be created. Configured folder = {}", sitemapRepositoryDestinationFolderName);
                throw new IllegalStateException("Cannot obtain folder to write sitemap files to.");
            }
            String sitemapFileName;
            for (Urlset urlset : getListOfSiteMaps()) {
                sitemapFileName = SITEMAP_FILENAME + getListOfSiteMaps().indexOf(urlset) + ".xml";
                // Write to the hippo repository
                writeAssetToRepository(sitemapFileName, assetsSiteMapRootFolder, SitemapGenerator.toString(urlset));
            }
        } else {
            LOG.error("Error while obtaing the sitemap folder from the repository. "
                    + "No assets will be created.");
            throw new IllegalStateException("Error while obtaing the sitemap folder from the repository. ");
        }
    }

    /**
     * Write the sitemap content to the repository.
     *
     * @param assetFilename           The name of the asset.
     * @param assetsRootFolder The root folder where the asset must be stored.
     * @param siteMapContent          The content of the asset.
     */
    public void writeAssetToRepository(
            final String assetFilename,
            final Node assetsRootFolder,
            final String siteMapContent) {
        try {
            if (assetsRootFolder.hasNode(assetFilename)) {
                // Remove the existing asset
                Node existingAsset = assetsRootFolder.getNode(assetFilename);
                existingAsset.remove();
            }
            Node hardhandle = assetsRootFolder.addNode(assetFilename, "hippo:handle");
            hardhandle.addMixin("hippo:hardhandle");

            Node asset = hardhandle.addNode(assetFilename, "hippogallery:exampleAssetSet");
            asset.addMixin("hippo:harddocument");
            Item item = asset.getPrimaryItem();
            if (item.isNode()) {
                Node primaryChild = (Node) item;
                if (primaryChild.isNodeType("hippo:resource")) {
                    primaryChild.setProperty("jcr:data", siteMapContent);
                    primaryChild.setProperty("jcr:mimeType", "application/xml");
                    primaryChild.setProperty("jcr:lastModified", (new Date()).getTime());
                }
            } else {
                LOG.error("Asset handle did not return a node as primary item, handle = {}, returned item = {}",
                        asset.getPath(), item.getPath());
                throw new IllegalStateException("Cannot save asset, because the primary item is not a node");
            }
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Error occurred while writing an asset to the repository", e);
            throw new IllegalStateException("Cannot save asset.", e);
        }
    }

    /**
     * Obtain the site map folder located in the assets folder in the repository.
     *
     * @return The asset sitemap folder represented as a node.
     */
    private Node obtainAssetSiteMapFolder() {
        try {
            Node assetsRoot = session.getNode(ASSETS_ROOT);
            Node node;

            if (assetsRoot.hasNode(ASSETS_SITEMAP_ROOT)) {
                // Remove the node - because it already exists.
                Node assetsSitemapRoot = assetsRoot.getNode(ASSETS_SITEMAP_ROOT);
                assetsSitemapRoot.remove();
            }
            node = assetsRoot.addNode(ASSETS_SITEMAP_ROOT, "hippostd:folder");
            node.addMixin("hippo:harddocument");
            session.save();
            return node;
        } catch (RepositoryException e) {
            LOG.error("Error occurred while writing the sitemap folder to the repository", e);
        }
        return null;
    }

    /**
     * Obtain the configured destination folder from the repository where all the sitemap files will be located.
     *
     * @param destinationFolder The configured folder.
     * @return The Jcr node representing the destination folder.
     */
    private Node obtainDestinationSiteMapFolder(final String destinationFolder) {
        try {
            Node rootDestination = assetsSiteMapRootFolder;
            Node node;
            if (!rootDestination.hasNode(destinationFolder)) {
                node = rootDestination.addNode(destinationFolder, "hippostd:folder");
                node.addMixin("hippo:harddocument");
                node.getSession().save();
            } else {
                node = rootDestination.getNode(destinationFolder);
            }
            return node;
        } catch (RepositoryException e) {
            LOG.error("Error occurred while writing the sitemap destination folder to the repository", e);
        }
        return null;
    }
}
