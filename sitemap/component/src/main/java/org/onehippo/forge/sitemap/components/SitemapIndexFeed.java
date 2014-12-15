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
package org.onehippo.forge.sitemap.components;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.forge.sitemap.generator.SitemapIndexGenerator;

/**
 * Serves a Sitemap IndexXML.
 *
 * @author Ricardo Sawirjo
 */
@SuppressWarnings({"unused" })
@ParametersInfo(type = SitemapIndexFeed.SitemapIndexFeedParametersInformation.class)
public class SitemapIndexFeed extends BaseHstComponent {
    /**
     * A sitemap index file will be created. This file will contain urls which points to the
     * assets folder in the Hippo Repository.
     * The sitemap-location-foldername must be configured via the console.
     * Add a parameter at the following location:
     * /hst:hst/hst:configurations/hst:default/hst:components/forge-sitemap-index-feed
     * The value of this parameter contains the complete folder path relative to
     * content/assets/sitemap/
     * The assets-url-prefix: this parameter is used for placing a prefix in front of the url, which is
     * linking to the asset. Why? Some sites uses url rewriting or mapping.
     * The additional-sitemap-file-url: this parameter is to be able to add manual additional
     * sitemap url to the sitemap index file. It can be a list of urls, where the items are separated with a comma.
     */
    public interface SitemapIndexFeedParametersInformation {
        /**
         * The location of the sitemap folder, where the sitemap files are stored.
         * These files will be used while generating the sitemap index file.
         * @return A string containing the location of the sitemap index files.
         */
        @Parameter(
                name = "sitemap-location-foldername",
                required = true
        )
        String getSitemapLocationFolderNameProperty();

        /**
         * For some systems the sitemap index files are stored in the repository asset folder.
         * These systems might use url rewriting. With this parameter a prefix can be configured, which
         * is placed in front of the asset url.
         * @return The prefix which is placed in front of the url.
         */
        @Parameter(
                name = "assets-url-prefix",
                required = false
        )
        String getAssetUrlPrefix();

        /**
         * Provide additional urls which must be added to the sitemap index.
         * @return A comma delimited list of urls which must be added to the generated site map index file.
         */
        @Parameter(
                name = "additional-sitemap-file-url",
                required = false
        )
        String getAdditionalSitemapFileUrl();
    }

    /**
     * @param request  The incoming request.
     * @param response The Response.
     * @throws HstComponentException
     */
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        SitemapIndexFeedParametersInformation parameters = getComponentParametersInfo(request);
        verifyRequiredParametersAreFilled(parameters);
        String siteMapFolder = parameters.getSitemapLocationFolderNameProperty();
        String urlPrefix = parameters.getAssetUrlPrefix();
        String additionalSiteMap = parameters.getAdditionalSitemapFileUrl();
        String [] additionalSiteMapUrls = null;
        if (isNotEmpty(additionalSiteMap)) {
            additionalSiteMapUrls = additionalSiteMap.split(",");
        }
        HippoBean assetContentRoot = getAssetBaseBean(request);
        request.setAttribute("sitemap", new SitemapIndexGenerator(request, assetContentRoot).
                createSitemapIndex(siteMapFolder, urlPrefix, additionalSiteMapUrls));
    }

    /**
     * Verify if the required parameters are filled via the CMS Console.
     * And that they are correct configured.
     *
     * @param parameters The parameters which have been configured in the CMS Console.
     */
    private static void verifyRequiredParametersAreFilled(final SitemapIndexFeedParametersInformation parameters) {
        if (StringUtils.isEmpty(parameters.getSitemapLocationFolderNameProperty())) {
            throw new HstComponentException("No sitemap location folder specified, please pass the parameter "
                    + "sitemap-location-foldername.");
        }
        if (isNotEmpty(parameters.getAssetUrlPrefix())) {
            String urlPrefix = parameters.getAssetUrlPrefix();
            if (isNotEmpty(urlPrefix) && (!urlPrefix.startsWith("/") || !urlPrefix.endsWith("/"))) {
                throw new HstComponentException("The assets-url-prefix specified, must start with a / and end with /.");
            }
        }
    }
}
