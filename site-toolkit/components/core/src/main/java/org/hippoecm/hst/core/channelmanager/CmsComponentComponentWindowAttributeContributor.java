/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_COMPONENT_EDITABLE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_EXPERIENCE_PAGE_COMPONENT;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

public class CmsComponentComponentWindowAttributeContributor implements ComponentWindowAttributeContributor {

    private final static Logger log = LoggerFactory.getLogger(CmsComponentComponentWindowAttributeContributor.class);

    @Override
    public void contributePreamble(HstComponentWindow window, HstRequest request, Map<String, String> populatingAttributesMap) {

        HstComponentConfiguration compConfig = ((HstComponentConfiguration) window.getComponentInfo());
        final HstRequestContext requestContext = request.getRequestContext();

        populatingAttributesMap.put("uuid", compConfig.getCanonicalIdentifier());

        try {
            final Session cmsUser = (Session)requestContext.getAttribute(ContainerConstants.CMS_USER_SESSION_ATTR_NAME);
            if (cmsUser == null) {
                throw new IllegalStateException("For Channel Manager preview requests there is expect to be a CMS user " +
                        "Session available");
            }

            final boolean inRole;

            if (compConfig.isExperiencePageComponent()) {
                populatingAttributesMap.put(HST_EXPERIENCE_PAGE_COMPONENT, "true");
                // check whether cmsUser has the right role on the xpage component
                inRole = isInRole(cmsUser, compConfig, XPAGE_REQUIRED_PRIVILEGE_NAME);
            } else {
                populatingAttributesMap.put(HST_EXPERIENCE_PAGE_COMPONENT, "false");
                // check whether cmsUser has the right role on the HST config component
                inRole = isInRole(cmsUser, compConfig, CHANNEL_WEBMASTER_PRIVILEGE_NAME);
            }

            populatingAttributesMap.put(HST_COMPONENT_EDITABLE, String.valueOf(inRole));

            // TODO instead of marking the component as locked the above should be enough but for now also mark it
            // TODO locked since the UI already 'knows' locked
            if (!inRole) {
                populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, "not enough karma");
            }

        } catch (PathNotFoundException e) {
            // cms user cannot read component thus certainly cannot modify it
            populatingAttributesMap.put(HST_COMPONENT_EDITABLE, "false");
        } catch (RepositoryException e) {
            log.error("RepositoryException for cmsUser session :", e);
            populatingAttributesMap.put(HST_COMPONENT_EDITABLE, "false");
        }

        if (compConfig.getXType() != null) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_XTYPE, compConfig.getXType());
        }
        if (compConfig.isInherited()) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_INHERITED, "true");
        }

        final String componentType = compConfig.getComponentType().toString();
        populatingAttributesMap.put(ChannelManagerConstants.HST_TYPE, componentType);
        populatingAttributesMap.put(ChannelManagerConstants.HST_LABEL, getLabel(compConfig));
        HstURLFactory urlFactory = requestContext.getURLFactory();
        HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, window.getReferenceNamespace(), null, requestContext);
        populatingAttributesMap.put("url", url.toString());
        populatingAttributesMap.put("refNS", window.getReferenceNamespace());

        final Channel channel = requestContext.getResolvedMount().getMount().getChannel();

        if (channel != null && channel.isConfigurationLocked() && !compConfig.isExperiencePageComponent()) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, "system");
            populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "false");
        } else if (compConfig.isExperiencePageComponent()) {
            // TODO lock the component in case an other user contains a lock (aka is editing the draft)
            // TODO see CMS-13158
        } else if (compConfig instanceof ConfigurationLockInfo) {
            ConfigurationLockInfo lockInfo = (ConfigurationLockInfo) compConfig;
            if (lockInfo.getLockedBy() != null) {
                String cmsUserId = (String) request.getAttribute(ContainerConstants.CMS_REQUEST_USER_ID_ATTR);
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

    private boolean isInRole(final Session cmsUser, final HstComponentConfiguration compConfig,
                             final String requiredPrivilege) throws RepositoryException {

        return Arrays.stream(cmsUser.getAccessControlManager()
                .getPrivileges(compConfig.getCanonicalStoredLocation()))
                .anyMatch(privilege -> privilege.getName().equals(requiredPrivilege));
    }

    @Override
    public void contributeEpilogue(HstComponentWindow window, HstRequest request, Map<String, String> populatingAttributesMap) {
        HstComponentConfiguration config = ((HstComponentConfiguration) window.getComponentInfo());

        populatingAttributesMap.put(ChannelManagerConstants.HST_END_MARKER, "true");
        populatingAttributesMap.put("uuid", config.getCanonicalIdentifier());
    }

    private String getLabel(HstComponentConfiguration config) {
        String label = config.getLabel();
        if (StringUtils.isBlank(label)) {
            label = config.getName();
        }
        return label;
    }
}
