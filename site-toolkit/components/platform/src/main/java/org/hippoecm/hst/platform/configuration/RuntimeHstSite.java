/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
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

import org.apache.commons.lang3.RegExUtils;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.InternalHstSiteMap;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.onehippo.cms7.services.hst.Channel;

public class RuntimeHstSite extends GenericHstSiteWrapper {

    private final HstSite delegatee;
    private final Channel channel;
    private final HstSiteMap hstSiteMap;
    private final HstSiteMenusConfiguration hstSiteMenusConfiguration;

    public RuntimeHstSite(final HstSite delegatee, final RuntimeMount runtimeMount) {
        super(delegatee);
        this.delegatee = delegatee;
        final Channel delegateeChannel = delegatee.getChannel();
        if (delegateeChannel == null) {
            channel = null;
            hstSiteMap = null;
            hstSiteMenusConfiguration = null;
        } else {
            channel = new Channel(delegateeChannel);
            final VirtualHost virtualHost = runtimeMount.getVirtualHost();
            channel.setHostGroup(virtualHost.getHostGroupName());
            channel.setHostname(virtualHost.getHostName());

            final String configUrl = channel.getUrl();

            final String runtimeUrl = RegExUtils.replaceFirst(configUrl,
                    runtimeMount.getDelegatee().getVirtualHost().getHostName(),
                    runtimeMount.getVirtualHost().getHostName());
            channel.setUrl(runtimeUrl);

            if (delegatee.getSiteMap() != null) {
                hstSiteMap = new RuntimeHstSiteMap((InternalHstSiteMap) delegatee.getSiteMap(), RuntimeHstSite.this);
            } else {
                hstSiteMap = null;
            }

            if (delegatee.getSiteMenusConfiguration() != null) {
                hstSiteMenusConfiguration = new RuntimeHstSiteMenusConfiguration(delegatee.getSiteMenusConfiguration(),
                        RuntimeHstSite.this);
            } else {
                hstSiteMenusConfiguration = null;
            }
        }

    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public HstSiteMap getSiteMap() {
        return hstSiteMap;
    }

    @Override
    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return hstSiteMenusConfiguration;
    }

    @Override
    public String toString() {
        return "RuntimeHstSite{" + "delegatee=" + delegatee + '}';
    }

}
