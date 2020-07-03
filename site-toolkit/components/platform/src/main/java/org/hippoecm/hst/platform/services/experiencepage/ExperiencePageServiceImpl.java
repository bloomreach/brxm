/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.services.experiencepage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.experiencepage.ExperiencePageLoadingException;
import org.hippoecm.hst.experiencepage.ExperiencePageService;
import org.hippoecm.hst.platform.configuration.cache.HstNodeImpl;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.XPAGE_PROPERTY_PAGEREF;
import static org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type.CONTAINER_COMPONENT;

public class ExperiencePageServiceImpl implements ExperiencePageService {

    private final static Logger log = LoggerFactory.getLogger(ExperiencePageServiceImpl.class);

    private final static String ROOT_EXPERIENCE_PAGES_NAME = "hst:experiencePage";

    public void init() {
        HippoServiceRegistry.register(this, ExperiencePageService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, ExperiencePageService.class);
    }

    @Override
    public HstComponentConfiguration loadExperiencePage(final Node hstPage, final ResolvedSiteMapItem resolvedSiteMapItem) {

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // set the classloader of the platform webapp to load the model
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            // Note that the 'hstPage' node can be a frozen node from version history for Experience Pages!!
            final HstNodeImpl hstNode = getHstNode(hstPage);

            final String pageref = hstNode.getValueProvider().getString(XPAGE_PROPERTY_PAGEREF);

            if (StringUtils.isBlank(pageref)) {
                throw new ExperiencePageLoadingException(String.format("Cannot load XPage since '%s' misses property value " +
                        "for '%s'", hstNode.getValueProvider().getPath(), XPAGE_PROPERTY_PAGEREF));
            }

            final HstSite hstSite = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap().getSite();
            final HstComponentsConfigurationService componentsConfiguration = (HstComponentsConfigurationService)hstSite.getComponentsConfiguration();
            final HstComponentConfigurationService xPageLayout =
                    (HstComponentConfigurationService) componentsConfiguration.getXPages().get(pageref);

            if (xPageLayout == null) {
                throw new ExperiencePageLoadingException(String.format("Cannot load XPage '%s' because XPage " +
                        "'%s' not found below at '%s'", hstNode.getValueProvider().getPath(),
                        pageref, hstSite.getConfigurationPath() + NODENAME_HST_PAGES));
            }

            final HstComponentConfigurationService copy = xPageLayout.copy(false);


            // the root configuration prefix is *JUST* the path to the hstPage itself (hstPage is typically a
            // node of type hst:component below a document variant
            final String rootConfigurationPrefix = hstNode.getValueProvider().getPath();


            final Map<String, HstNode> documentContainersNodes = hstNode.getChildren();

            // get the hst containers of an XPage below a document variant in isolation!
            // the 'keys' are the node names of 'containers' below the XPage document variant which MAP to the
            // hst:qualifier in the hst:containercomponent in the XPage hst configuration!
            //final Map<String, HstComponentConfigurationService> containerConfigurations = containers.entrySet().stream()

            final Map<String, HstComponentConfigurationService> documentContainers = documentContainersNodes.entrySet().stream()
                    .filter(entry -> {
                        final boolean container = NODETYPE_HST_CONTAINERCOMPONENT.equals(entry.getValue().getNodeTypeName());
                        if (!container) {
                            log.info("Skip Node : only container node types allowed below hst:xpage document but found node of type " +
                                    "'{}' for '{}'", entry.getValue().getNodeTypeName(), entry.getValue().getValueProvider().getPath());
                        }
                        return container;
                    })
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry ->
                            new HstComponentConfigurationService(entry.getValue(), null, ROOT_EXPERIENCE_PAGES_NAME, Collections.emptyMap(), rootConfigurationPrefix)));



            // mark the container items as experience page components
            documentContainers.values().forEach(config -> config.flattened().forEach(c ->
                    ((HstComponentConfigurationService)c).setExperiencePageComponent(true)));

            // into the 'copy' of the xpage, now glue the 'containerConfigurations' from the XPage document variant:
            // it is however not a full replacement since all configuration from the 'hst config containers' should be
            // kept EXCEPT the canonical stored path and identifier

            // first collect and do not manipulate in stream since then ConcurrentModificationException can happen
            // because component items get replaced
            final List<HstComponentConfiguration> flattened = copy.flattened().collect(Collectors.toList());

            final List<String> mergedDocumentContainers = new ArrayList<>();

            for (HstComponentConfiguration c : flattened) {

                final HstComponentConfigurationService pageLayoutContainer = (HstComponentConfigurationService) c;
                if (!pageLayoutContainer.isXpageLayoutComponent()) {
                    // If the container is a NON-EXPLICIT-XPAGE-LAYOUT container, for example inherited 'header' container
                    // it does not need transformation to Experience Page Container : We do not TRANSFORM such components
                    // Note that in the future we might want to support it: then for example a shared header container
                    // can be hijacked in an XPage document through the qualifier id
                    continue;
                }

                if (pageLayoutContainer.getComponentType() != CONTAINER_COMPONENT) {
                    continue;
                }


                // found a container in the XPage hst configuration : Replace it with the 'container' from the
                // XPAGE document variant. If the XPage document variant DOES not (yet) have the container (since
                // for example later added to the XPage hst config), then add the 'container' empty with the
                // UUID of the hst config XPage container: Then WHEN in the CM a webmaster interacts with that
                // container, the Page Composer code will add the container to the XPage!
                final String qualifier = pageLayoutContainer.getQualifier();

                if (qualifier == null) {
                    // check whether the page layout container was coming from, say, abstractpages or from an
                    // Experience Page
                    log.warn("Experience Page Container component should have property '{}' but is missing. Remove " +
                            "container from request time XPage configuration", pageLayoutContainer.getCanonicalStoredLocation());
                    ((HstComponentConfigurationService)pageLayoutContainer.getParent()).removeChild(pageLayoutContainer);
                }


                final HstComponentConfigurationService documentContainer = documentContainers.get(qualifier);
                if (documentContainer == null) {
                    log.debug("XPage Document container for component '{}' does not exist, use the container " +
                            "from the HST Page layout config so items can be added via CM still", pageLayoutContainer.getCanonicalStoredLocation());

                    pageLayoutContainer.transformXpageLayoutContainer();
                } else {
                    mergedDocumentContainers.add(qualifier);
                    // Replace the UUID and canonical stored location of the container and add the XPage document
                    // children
                    pageLayoutContainer.transformXpageLayoutContainer(documentContainer);

                    // enhance the not yet enhanced XPage container items
                    componentsConfiguration.enhanceComponentTree(documentContainer.getChildren().values());

                    // TODO for the new feature of container items backreferencing the catalog item, make sure
                    // TODO the catalog item inheritance works
                }

            }

            if (log.isInfoEnabled()) {
                mergedDocumentContainers.forEach(qualifier -> documentContainers.remove(qualifier));

                // any left documentContainers are container that are not present (any more) in the XPAge Layout. Log an
                // info message about these
                documentContainers.values().forEach(config -> log.info("Document XPage contains container '{}' which " +
                        "is not represented by any hst:qualifier in XPage Layout '{}' and will as a result be ignored",
                        config.getCanonicalStoredLocation(), xPageLayout.getCanonicalStoredLocation()));
            }

            return copy;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }


    }

    private HstNodeImpl getHstNode(final Node hstPage) {
        try {

            // The hstPage can be a frozen node and then every property is protected, hence we need to include
            // protected properties (which we don't want for in memory model since takes pointless memory but does not
            // matter for Experience Pages since used only once
            return new HstNodeImpl(hstPage, null, true);

        } catch (RepositoryException e) {
            throw new ExperiencePageLoadingException("Failed to load HstPage" , e);
        }
    }

}
