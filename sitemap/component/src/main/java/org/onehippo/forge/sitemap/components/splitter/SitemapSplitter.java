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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.site.HstServices;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * According to the sitemap protocol a sitemap file may not contain more than 50,000 URLs.
 * The SitemapSplitter will split the sitemap in multiple sitemap files in case the
 * base sitemap contains more than 50,000 URLs.
 * The different sitemap files will be written to the Hippo Repository to a location,
 * which must be configured, via the Hippo console.
 * For now the configuration for this folder handles only a depth of one.
 * The default location, if the destination folder is not configured, is
 * /content/assets/sitemap otherwise it will be /content/assets/sitemap/foldername.
 * Note: A SiteMapIndex file must be created which will contain the different SiteMap files.
 */
public abstract class SitemapSplitter {

    static final Logger log = LoggerFactory.getLogger(SitemapSplitter.class);

    private List<Url> urls;
    private List<Urlset> listOfSiteMaps;

    /**
     * The sitemap splitter accepts a list of {@link Url}.
     * @param urls This list of {@link Url} will be split.
     */
    protected SitemapSplitter(final List<Url> urls) {
        this.urls = urls;
    }

    /**
     * Split the sitemap if the number of urls exceeds the MAX_URLS.
     * Besides the split into different sitemap files, these files will be written to the
     * assets folder in the repository.
     *
     * @return True if the split was executed, else return false.
     */
    public boolean split() {
        int numberOfUrls = urls.size();

        // write a set of MAX_URLS urls to a file
        listOfSiteMaps = new ArrayList<Urlset>();
        int numberOfMaximumFilledSiteMapFiles = numberOfUrls / Urlset.MAX_SUPPORTED_URLS_PER_FILE;
        int numberOfLeftOverUrls = numberOfUrls % Urlset.MAX_SUPPORTED_URLS_PER_FILE;
        /**
         * Determine the different subsets.
         * Create subsets based upon the from and the to index.
         */
        int fromIndex;

        int toIndex;
        for (int sitemapCount = 1; sitemapCount <= numberOfMaximumFilledSiteMapFiles; sitemapCount++) {
            fromIndex = (sitemapCount - 1) * Urlset.MAX_SUPPORTED_URLS_PER_FILE;
            toIndex = Urlset.MAX_SUPPORTED_URLS_PER_FILE * sitemapCount;
            List<Url> subsetOfUrlSet = urls.subList(fromIndex, toIndex);
            listOfSiteMaps.add(new Urlset(subsetOfUrlSet));
        }
        if (numberOfLeftOverUrls > 0) {
            // In case there are urls leftover we must create the leftover subset.
            int finalFromIndex = (numberOfMaximumFilledSiteMapFiles) * Urlset.MAX_SUPPORTED_URLS_PER_FILE;
            List<Url> finalSubsetOfUrlSet = urls.subList(finalFromIndex, numberOfUrls);
            listOfSiteMaps.add(new Urlset(finalSubsetOfUrlSet));
        }

        try {
            writeSiteMapFilesToDestination();
        } catch (Exception e) {
            log.error("Cannot write site map files to their destination", e);
            return false;
        }
        return true;
    }

    /**
     * Subclasses should implement their own method to write the splitted sitemap to a destination.
     */
    public abstract void writeSiteMapFilesToDestination();

    public List<Urlset> getListOfSiteMaps() {
        return listOfSiteMaps;
    }
}
