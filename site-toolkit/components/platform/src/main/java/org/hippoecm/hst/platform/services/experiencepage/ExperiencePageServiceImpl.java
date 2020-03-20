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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.platform.configuration.cache.HstNodeImpl;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.experiencepage.ExperiencePageService;
import org.hippoecm.hst.experiencepage.ExperiencePageLoadingException;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class ExperiencePageServiceImpl implements ExperiencePageService {

    private final static String ROOT_EXPERIENCE_PAGES_NAME = "hst:experiencePage";

    public void init() {
        HippoServiceRegistry.register(this, ExperiencePageService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, ExperiencePageService.class);
    }

    @Override
    public HstComponentConfiguration loadExperiencePage(final Node hstPage, final ResolvedSiteMapItem resolvedSiteMapItem) {


        HstNodeImpl hstNode = getHstNode(hstPage);

        // the root configuration prefix is *JUST* the path to the hstPage itself (hstPage is typically a
        // node of type hst:component below a document variant
        final String rootConfigurationPrefix = hstNode.getValueProvider().getPath();

        // just like explicit pages below the workspace do not support referenceable containers (aka components that
        // are of type 'hst:containercomponentreference', we also (of course) do not support this below hst:page
        // nodes below eg a hippo document variant: All the hst component configuration changes for that hst:page
        // are explicitly below the hst:page node and not to some referenceable container
        final Map<String, HstNode> referenceableContainers = Collections.emptyMap();

        final HstComponentConfigurationService experiencePageComponentConfig =
                new HstComponentConfigurationService(hstNode, null, ROOT_EXPERIENCE_PAGES_NAME, referenceableContainers, rootConfigurationPrefix);

        // mark all the canonically present hst component nodes below the experience page to have 'experiencePageComponent = true'
        markAllExperienceComponents(experiencePageComponentConfig);

        final HstSite site = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap().getSite();
        final HstComponentsConfigurationService hstComponentsConfigurationService = (HstComponentsConfigurationService) site.getComponentsConfiguration();

        hstComponentsConfigurationService.populateExperiencePage(experiencePageComponentConfig);


        return experiencePageComponentConfig;


    }

    private HstNodeImpl getHstNode(final Node hstPage) {
        try {
            return new HstNodeImpl(hstPage, null);
        } catch (RepositoryException e) {
            throw new ExperiencePageLoadingException("Failed to load HstPage" , e);
        }
    }

    // mark the experience page components
    private void markAllExperienceComponents(final HstComponentConfigurationService newComponentConfig) {
        newComponentConfig.flattened().forEach(comp -> ((HstComponentConfigurationService) comp).setExperiencePageComponent(true));
    }
}
