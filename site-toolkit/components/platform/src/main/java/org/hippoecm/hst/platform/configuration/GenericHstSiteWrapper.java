/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.onehippo.cms7.services.hst.Channel;

public class GenericHstSiteWrapper implements HstSite {

    private HstSite delegatee;

    public GenericHstSiteWrapper(final HstSite delegatee) {

        this.delegatee = delegatee;
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public String getCanonicalIdentifier() {
        return delegatee.getCanonicalIdentifier();
    }

    @Override
    public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration() {
        return delegatee.getSiteMapItemHandlersConfiguration();
    }

    @Override
    public HstComponentsConfiguration getComponentsConfiguration() {
        return delegatee.getComponentsConfiguration();
    }

    @Override
    public HstSiteMap getSiteMap() {
        return delegatee.getSiteMap();
    }

    @Override
    public LocationMapTree getLocationMapTree() {
        return delegatee.getLocationMapTree();
    }

    @Override
    public LocationMapTree getLocationMapTreeComponentDocuments() {
        return delegatee.getLocationMapTreeComponentDocuments();
    }

    @Override
    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return delegatee.getSiteMenusConfiguration();
    }

    @Override
    public String getConfigurationPath() {
        return delegatee.getConfigurationPath();
    }

    @Override
    public boolean hasPreviewConfiguration() {
        return delegatee.hasPreviewConfiguration();
    }

    @Override
    public Channel getChannel() {
        return delegatee.getChannel();
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo() {
        return delegatee.getChannelInfo();
    }
}
