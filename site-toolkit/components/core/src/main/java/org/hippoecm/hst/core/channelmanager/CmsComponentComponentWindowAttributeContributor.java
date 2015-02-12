/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.channelmanager;

import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class CmsComponentComponentWindowAttributeContributor implements ComponentWindowAttributeContributor {

    @Override
    public void contribute(HstComponentWindow window, HstRequest request, Map<String, String> populatingAttributesMap) {

        HstComponentConfiguration compConfig = ((HstComponentConfiguration) window.getComponentInfo());
        final HstRequestContext requestContext = request.getRequestContext();

        populatingAttributesMap.put("uuid", compConfig.getCanonicalIdentifier());
        if (compConfig.getXType() != null) {
            populatingAttributesMap.put("xtype", compConfig.getXType());
        }
        if (compConfig.isInherited()) {
            populatingAttributesMap.put("inherited", "true");
        }
        populatingAttributesMap.put("type", compConfig.getComponentType().toString());
        HstURLFactory urlFactory = requestContext.getURLFactory();
        HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, window.getReferenceNamespace(), null, requestContext);
        populatingAttributesMap.put("url", url.toString());
        populatingAttributesMap.put("refNS", window.getReferenceNamespace());
        if (compConfig instanceof ConfigurationLockInfo) {
            ConfigurationLockInfo lockInfo = (ConfigurationLockInfo) compConfig;
            if (lockInfo.getLockedBy() != null) {
                String cmsUserId = (String) request.getSession(false).getAttribute(ContainerConstants.CMS_USER_ID_ATTR);
                populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, lockInfo.getLockedBy());
                if (lockInfo.getLockedBy().equals(cmsUserId)) {
                    populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "true");
                } else {
                    populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "false");
                }
                if (lockInfo.getLockedOn() != null) {
                    populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_ON, String.valueOf(lockInfo.getLockedOn().getTimeInMillis()));
                }
            }
        }
        if (compConfig.getLastModified() != null) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_LAST_MODIFIED, String.valueOf(compConfig.getLastModified().getTimeInMillis()));
        }

    }
}
