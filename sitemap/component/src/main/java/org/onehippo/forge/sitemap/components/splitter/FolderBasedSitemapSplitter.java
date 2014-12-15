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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.onehippo.forge.sitemap.components.model.sitemapindex.SitemapIndex;
import org.onehippo.forge.sitemap.components.model.sitemapindex.TSitemap;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;
import org.onehippo.forge.sitemap.generator.SitemapIndexGenerator;

/**
 * A site map splitter that writes the files to a folder. The implementing class should implement the writeToFolder()
 * method to write the files to an actual folder.
 */
public abstract class FolderBasedSitemapSplitter extends SitemapSplitter {

    private static final String SITEMAP_FILENAME = "sitemap-index-";

    /**
     * The sitemap splitter accepts a list of {@link org.onehippo.forge.sitemap.components.model.Url}.
     *
     * @param urls This list of {@link org.onehippo.forge.sitemap.components.model.Url} will be split.
     */
    protected FolderBasedSitemapSplitter(final List<Url> urls) {
        super(urls);
    }

    @Override
    public void writeSiteMapFilesToDestination() {
        // We must write the sub sitemaps to different files.
        // A sitemap index file must be created via a separate process and will not be created
        // at this moment.
        SitemapIndex sitemapIndexFile = new SitemapIndex();
        for (Urlset urlset : getListOfSiteMaps()) {
            String sitemapFileName = SITEMAP_FILENAME + getListOfSiteMaps().indexOf(urlset) + ".xml";
            writeToFolder(SitemapGenerator.toString(urlset),
                    sitemapFileName);
            sitemapIndexFile.getSitemap().add(createSitemapIndexItem(sitemapFileName));
        }
        if (!sitemapIndexFile.getSitemap().isEmpty()) {
            writeToFolder(SitemapIndexGenerator.toString(sitemapIndexFile), "sitemapindex.xml");
        }
    }

    /**
     * Create an item in the Sitemap Index file.
     *
     * @param sitemapFile The name of the file which will be converted into the sitemap index file.
     * @return The Sitemap Index file item.
     */
    private static TSitemap createSitemapIndexItem(final String sitemapFile) {
        TSitemap tSitemap = new TSitemap();
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        tSitemap.setLoc(sitemapFile);
        tSitemap.setLastmod(getCurrentDateTime(now));
        return tSitemap;
    }

    private static String getCurrentDateTime(final Calendar calendar) {
        FastDateFormat dateFormatter = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
        return dateFormatter.format(calendar);
    }

    protected abstract void writeToFolder(String content, String filename);
}
