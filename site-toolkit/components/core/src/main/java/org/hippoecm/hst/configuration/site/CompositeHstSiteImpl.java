/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.Map;

import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeHstSiteImpl implements CompositeHstSite {

    private static final Logger log = LoggerFactory.getLogger(CompositeHstSiteImpl.class);

    private final HstSite master;
    private final Map<String, HstSite> branches;
    private final DelegatingHstSiteProvider delegatingHstSiteProvider;

    public CompositeHstSiteImpl(final HstSite master, final Map<String, HstSite> branches) {
        this.master = master;
        this.branches = branches;
        // TODO HSTTWO-4355 Most likely not ok like this!
        delegatingHstSiteProvider = HstServices.getComponentManager().getComponent(DelegatingHstSiteProvider.class);
        if (delegatingHstSiteProvider == null) {
            log.warn("No DelegatingHstSiteProvider is found.");
        }
    }

    public HstSite getMaster() {
        return master;
    }

    public Map<String, HstSite> getBranches() {
        return branches;
    }

    private HstSite getActiveHstSite() {
        if (branches.isEmpty() || delegatingHstSiteProvider == null) {
            return master;
        }
        return delegatingHstSiteProvider.getHstSite(this, RequestContextProvider.get());
    }

    @Override
    public String getName() {
        return getActiveHstSite().getName();
    }

    @Override
    public String getCanonicalIdentifier() {
        return getActiveHstSite().getCanonicalIdentifier();
    }

    @Override
    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration() {
        return getActiveHstSite().getSiteMapItemHandlersConfiguration();
    }

    @Override
    public HstComponentsConfiguration getComponentsConfiguration() {
        return getActiveHstSite().getComponentsConfiguration();
    }

    @Override
    public HstSiteMap getSiteMap() {
        return getActiveHstSite().getSiteMap();
    }

    @Override
    public LocationMapTree getLocationMapTree() {
        return getActiveHstSite().getLocationMapTree();
    }

    @Override
    public LocationMapTree getLocationMapTreeComponentDocuments() {
        return getActiveHstSite().getLocationMapTreeComponentDocuments();
    }

    @Override
    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return getActiveHstSite().getSiteMenusConfiguration();
    }

    @Override
    public String getConfigurationPath() {
        return getActiveHstSite().getConfigurationPath();
    }

    @Override
    @Deprecated
    public long getVersion() {
        return getActiveHstSite().getVersion();
    }

    @Override
    public boolean hasPreviewConfiguration() {
        return getActiveHstSite().hasPreviewConfiguration();
    }

    @Override
    public Channel getChannel() {
        return getActiveHstSite().getChannel();
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo() {
        return getActiveHstSite().getChannelInfo();
    }

    @Override
    public String toString() {
        return "CompositeHstSiteImpl{" +
                "master=" + master +
                ", branches=" + branches +
                '}';
    }
}