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
import java.util.Set;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.linking.LocationMapTree;

public class CompositeHstSite implements HstSite {

    private final HstSite master;
    private final Set<HstSite> branches;
    public CompositeHstSite(final HstSite master, final Set<HstSite> branches) {
        this.master = master;
        this.branches = branches;
    }

    public HstSite getMaster() {
        return master;
    }

    public Set<HstSite> getBranches() {
        return branches;
    }

    public HstSite getActiveHstSite() {
        if (branches.isEmpty()) {
            return master;
        }
        // TODO HSTTWO-3983
        return master;
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
}