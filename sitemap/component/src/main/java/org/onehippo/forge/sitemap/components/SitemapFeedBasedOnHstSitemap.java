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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.onehippo.forge.sitemap.components.util.SiteMapGeneratorUtils.createUrlInformationProvider;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstComponentFatalException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.onehippo.forge.sitemap.components.splitter.FileSystemSitemapSplitter;
import org.onehippo.forge.sitemap.components.splitter.RepositorySitemapSplitter;
import org.onehippo.forge.sitemap.components.splitter.SitemapSplitter;
import org.onehippo.forge.sitemap.components.util.OutputMode;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = SitemapFeedBasedOnHstSitemap.SiteMapFeedBasedOnHstSiteMapParameters.class)
public class SitemapFeedBasedOnHstSitemap extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(SitemapFeedBasedOnHstSitemap.class);
    private static final String REGEX_FOR_SPLITTING_COMMA_SEPERATED_PARAMETERS = "[\\s]*,[\\s]*";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        
        super.doBeforeRender(request, response);

        SiteMapFeedBasedOnHstSiteMapParameters parameters = getComponentParametersInfo(request);

        SitemapGenerator sitemapGenerator = createSitemapGenerator(request.getRequestContext(), parameters);
        Urlset urlset = sitemapGenerator.createUrlSetBasedOnHstSiteMap();
        boolean splittingExecuted;

        OutputMode outputMode = getOutputMode(parameters);

        splittingExecuted = outputMode.shouldSplit()
                && splitSiteMapAndWriteToDestination(request, parameters, urlset);

        logQueryStatistics(sitemapGenerator);

        if (!splittingExecuted) {
            if (urlset.getUrls().size() > Urlset.MAX_SUPPORTED_URLS_PER_FILE) {
                throw new HstComponentFatalException("Sitemap was not split, but contains more than "
                        + Urlset.MAX_SUPPORTED_URLS_PER_FILE + " entries, configure the splitter.");
            }
            String siteMapXml = SitemapGenerator.toString(urlset);
            request.setAttribute("sitemap", siteMapXml);
        } else {
            if (!outputMode.outputsToResponse()) {
                request.setAttribute("sitemap", "Site map succesfully split, please retrieve the index..");
            }
        }
    }

    private static void logQueryStatistics(final SitemapGenerator sitemapGenerator) {
        log.info("-----------");
        log.info("Queries fired: " + sitemapGenerator.getQueriesFired());
        log.info("Cache Hits  : " + sitemapGenerator.getQueryCacheHits());
        log.info("-----------");
    }

    @SuppressWarnings({"deprecation"})
    private static OutputMode getOutputMode(final SiteMapFeedBasedOnHstSiteMapParameters parameters) {
        if (isNotBlank(parameters.getOutputMode())) {
            if (!OutputMode.existsForString(parameters.getOutputMode())) {
                log.error("Output mode '{}' does not exist.", parameters.getOutputMode());
                throw new HstComponentFatalException("Specified output mode does not exist");
            }
            return OutputMode.valueOf(parameters.getOutputMode().trim());
        } else {
            // Here for backwards compatibility
            log.info("Reverting to deprecated way of detecting the output mode, please use the property 'output-mode'."
                    + "It allows easier configuration than 'write-to-repository' and 'splitter-enabled'. For now the"
                    + "old way of configuration is supported.");
            if (parameters.isSitemapSplitterEnabled()) {
                return parameters.isWriteToRepositoryEnabled() ?
                        OutputMode.SPLIT_TO_REPOSITORY : OutputMode.SPLIT_TO_FILE_SYSTEM;
            } else {
                return OutputMode.STREAM_SITE_MAP;
            }
        }
    }

    private SitemapGenerator createSitemapGenerator(final HstRequestContext requestContext,
                                                    final SiteMapFeedBasedOnHstSiteMapParameters parameters) {
        String[] refIdsToIgnore = parameters.getSitemapExclusionsForRefIds().trim()
                .split(REGEX_FOR_SPLITTING_COMMA_SEPERATED_PARAMETERS);
        String[] componentConfigurationIdsToIgnore = parameters.getSitemapExclusionsForComponentConfigurationIds()
                .trim().split(REGEX_FOR_SPLITTING_COMMA_SEPERATED_PARAMETERS);
        String[] sitemapPathExclusions = parameters.getSiteMapExclusionsForSiteMapPath()
                .trim().split(REGEX_FOR_SPLITTING_COMMA_SEPERATED_PARAMETERS);
        int amountOfWorkers = parameters.getAmountOfWorkersForSiteMap();

        UrlInformationProvider informationProvider = createUrlInformationProvider(parameters.getInformationProvider());

        SitemapGenerator sitemapGenerator =
                new SitemapGenerator(requestContext, RequestContextProvider.get().getContentBeansTool().getObjectConverter(), informationProvider);

        sitemapGenerator.addSitemapRefIdExclusions(refIdsToIgnore);
        sitemapGenerator.addComponentConfigurationIdExclusions(componentConfigurationIdsToIgnore);
        sitemapGenerator.addSitemapPathExclusions(sitemapPathExclusions);
        sitemapGenerator.setAmountOfWorkers(amountOfWorkers);
        return sitemapGenerator;
    }

    private boolean splitSiteMapAndWriteToDestination(final HstRequest request,
                                                      final SiteMapFeedBasedOnHstSiteMapParameters parameters,
                                                      final Urlset urlset) {
        OutputMode outputMode = getOutputMode(parameters);
        SitemapSplitter splitter;
        switch (outputMode) {
            case SPLIT_TO_FILE_SYSTEM:
                splitter = new FileSystemSitemapSplitter(urlset, parameters.getSitemapDestinationFolderNameProperty());
                return splitter.split();
            case SPLIT_TO_REPOSITORY:
                Session writeSession = null;
                try {
                    /**
                     * A persistable session is needed in order to be able to write to the repository.
                     * In the hst-config.properties a user is needed with writable permissions.
                     */
                    writeSession = getPersistableSession(request);
                    splitter = new RepositorySitemapSplitter(urlset, writeSession,
                            parameters.getSitemapDestinationFolderNameProperty());

                    return splitter.split();
                } catch (RepositoryException e) {
                    throw new HstComponentFatalException("Cannot get a writable session", e);
                } finally {
                    if (writeSession != null) {
                        writeSession.logout();
                    }
                }
            case SPLIT_TO_TAR_GZ_STREAM:
                log.error("Splitting to a tar.gz stream is not supported, please use the JAX-RS resource.");
                return false;
            default:
                log.error("No mode that supports splitting specified, cannot split site map. Mode = {}", outputMode);
                return false;
        }
    }

    /**
     * Parameters to be configured via the Hippo Console. With these parameters it can be indicated to the sitemap
     * generator what must be excluded from the sitemap, activate the sitemap splitter, indicate where to write the
     * splitted sitemap files or indicate if the splitted sitemap files must be written to the repository or not.
     */
    public interface SiteMapFeedBasedOnHstSiteMapParameters {
        /**
         * Ref Id-s which must be excluded from the generated sitemap.
         *
         * @return The list of ref id-s which must be excluded.
         */
        @Parameter(name = "sitemapRefIdExclusions", defaultValue = "")
        String getSitemapExclusionsForRefIds();

        /**
         * The component configuration Ids which must be excluded from the generated sitemap.
         *
         * @return the list of component configuration Ids which must be excluded.
         */
        @Parameter(name = "sitemapComponentConfigurationIdExclusions", defaultValue = "")
        String getSitemapExclusionsForComponentConfigurationIds();

        /**
         * The sitemap paths which must be excluded from the generated sitemap.
         *
         * @return the list of sitemap paths which must be excluded.
         */
        @Parameter(name = "sitemapPathExclusions", defaultValue = "")
        String getSiteMapExclusionsForSiteMapPath();

        /**
         * The {@link UrlInformationProvider}.
         *
         * @return the information provider
         */
        @Parameter(name = "informationProvider", defaultValue = "")
        String getInformationProvider();

        /**
         * The folder name  where the splitted sitemap files will be stored.
         *
         * @return The name of the folder.
         */
        @Parameter(name = "splitter-destination-foldername")
        String getSitemapDestinationFolderNameProperty();

        /**
         * Indicate if the splitter must be enabled or not.
         *
         * @return True if enabled and false if not enabled.
         */
        @Deprecated
        @Parameter(name = "splitter-enabled", defaultValue = "false")
        boolean isSitemapSplitterEnabled();

        /**
         * The output mode to use
         *
         * @return String representation of the enum {@link OutputMode}
         */
        @Parameter(name = "output-mode", defaultValue = "")
        String getOutputMode();

        @Deprecated
        @Parameter(name = "write-to-repository", defaultValue = "false")
        boolean isWriteToRepositoryEnabled();

        @Parameter(name = "amountOfWorkers", defaultValue = "4")
        int getAmountOfWorkersForSiteMap();
    }

}
