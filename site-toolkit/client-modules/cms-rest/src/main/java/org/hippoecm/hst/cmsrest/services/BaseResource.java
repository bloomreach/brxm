/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.services;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;

import static org.hippoecm.hst.cmsrest.container.CmsRestSecurityValve.HOST_GROUP_NAME_FOR_CMS_HOST;

/**
 * An abstract base class represents functionality common among different RESTful resources
 */
public abstract class BaseResource {

    protected ChannelManager channelManager;
    protected Predicate<Channel> channelFilter;

    protected static VirtualHosts getVirtualHosts() {
        return RequestContextProvider.get().getVirtualHost().getVirtualHosts();
    }

    public String getHostGroupNameForCmsHost() {
        String hostGroupNameForCmsHost = (String)RequestContextProvider.get().getAttribute(HOST_GROUP_NAME_FOR_CMS_HOST);
        if (hostGroupNameForCmsHost == null) {
            throw new IllegalStateException("For cms rest request there should be a request context attr for '"+HOST_GROUP_NAME_FOR_CMS_HOST+"' " +
                    "but wasn't found.");
        }
        return hostGroupNameForCmsHost;
    }
    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setChannelFilter(final Predicate<Channel> channelFilter) {
        this.channelFilter = channelFilter;
    }


}
