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
package org.onehippo.forge.sitemap.generator;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.model.sitemapindex.SitemapIndex;
import org.onehippo.forge.sitemap.components.model.sitemapindex.TSitemap;

/**
 * Creates a SitemapIndex file when there are multiple sitemap files available.
 *
 * @author Ricardo Sawirjo
 */
public class SitemapIndexGenerator {
    private final HstLinkCreator linkCreator;
    private final HstRequestContext requestContext;
    private final HippoBean assetContentRoot;

    /**
     * A factory for the Sitemap Index.
     *
     * @param request          The HstRequest.
     * @param assetContentRoot Contains the folder where the sitemaps are stored relative to /assets/sitemap
     */
    public SitemapIndexGenerator(final HstRequest request, final HippoBean assetContentRoot) {
        this.assetContentRoot = assetContentRoot;
        this.requestContext = request.getRequestContext();
        this.linkCreator = requestContext.getHstLinkCreator();
    }

    /**
     * Create the sitemapIndex file.
     *
     * @param siteMapFolder  The folder where all the sitemap files are stored in the assets/sitemap folder in the repo.
     * @param assetUrlPrefix A prefix which is appended in front of the url - objective: to resolve the sitemap asset.
     * @param additionalSitemapFileUrls an Arrays of additional Urls pointing to sitemap files to add to the sitemap
     * index.
     * @return The SitemapIndex XML file as string.
     */
    public String createSitemapIndex(final String siteMapFolder,
                                     final String assetUrlPrefix,
                                     final String[] additionalSitemapFileUrls) {
        SitemapIndex sitemapIndex = new SitemapIndex();
        HippoBean scope = assetContentRoot.getBean(siteMapFolder);
        List<HippoBean> sitemapAssets = scope.getChildBeans("hippogallery:exampleAssetSet");
        for (HippoBean assetBean : sitemapAssets) {
            HstLink sitemapLink = linkCreator.create((HippoBean)assetBean.getCanonicalBean(), requestContext);
            rewriteAssetLink(sitemapLink, assetUrlPrefix);
            String sitemapLinkUrl = sitemapLink.toUrlForm(requestContext, true);
            TSitemap tSitemap = createSitemapIndexItem(sitemapLinkUrl);
            sitemapIndex.getSitemap().add(tSitemap);
        }
        // Add additional sitemap urls - these were manually added via the console.
        if (additionalSitemapFileUrls != null) {
            for (String additionalSitemapFileUrl : additionalSitemapFileUrls) {
                TSitemap tSitemap = createSitemapIndexItem(additionalSitemapFileUrl.trim());
                sitemapIndex.getSitemap().add(tSitemap);
            }
        }
        return toString(sitemapIndex);
    }

    /**
     * The link to the asset must be rewritten. Depending on the site a prefix must be appended in front of the url.
     *
     * @param hstLink   The link that must be rewritten.
     * @param urlPrefix The prefix which must be appended in front of the url.
     */
    private void rewriteAssetLink(final HstLink hstLink, final String urlPrefix) {
        String cleanPath = hstLink.getPath();
        String newPath = urlPrefix + cleanPath;
        hstLink.setPath(newPath);
    }

    /**
     * Create the sitemapIndex item.
     *
     * @param sitemapFileUrl The url of the asset.
     * @return The list of sitemapIndex items
     */
    private TSitemap createSitemapIndexItem(final String sitemapFileUrl) {
        TSitemap tSitemap = new TSitemap();
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        tSitemap.setLoc(sitemapFileUrl);
        tSitemap.setLastmod(getCurrentDateTime(now));
        return tSitemap;
    }

    /**
     * Parse the Sitemapindex to an xml document.
     *
     * @param sitemapindex The sitemapindex contains the content which must be converted.
     * @return The string containing the SitemapIndex XML file content.
     */
    public static String toString(final SitemapIndex sitemapindex) {
        String output;
        try {
            JAXBContext jc = JAXBContext.newInstance(SitemapIndex.class, TSitemap.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
            StringWriter sw = new StringWriter();
            m.marshal(sitemapindex, sw);
            output = sw.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot marshal the SitemapIndex to an XML string", e);
        }
        return output;
    }

    /**
     * Convert the given calendar date to a format, which complies to the ISO Time Zone format.
     *
     * @param calendar The calendar which must be used.
     * @return A String containing the date in the correct time zone format.
     */
    private String getCurrentDateTime(final Calendar calendar) {
        FastDateFormat dateFormatter = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
        return dateFormatter.format(calendar);
    }
}
