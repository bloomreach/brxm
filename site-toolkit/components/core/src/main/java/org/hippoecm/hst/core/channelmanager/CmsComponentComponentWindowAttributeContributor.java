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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.FALSE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_EXPERIENCE_PAGE_COMPONENT;
import static org.hippoecm.repository.api.DocumentWorkflowAction.obtainEditableInstance;

public class CmsComponentComponentWindowAttributeContributor implements ComponentWindowAttributeContributor {

    private final static Logger log = LoggerFactory.getLogger(CmsComponentComponentWindowAttributeContributor.class);

    @Override
    public void contributePreamble(HstComponentWindow window, HstRequest request, Map<String, String> populatingAttributesMap) {

        final HstComponentConfiguration compConfig = ((HstComponentConfiguration) window.getComponentInfo());
        final HstRequestContext requestContext = request.getRequestContext();

        try {

            final HippoSession cmsUser = (HippoSession) requestContext.getAttribute(ContainerConstants.CMS_USER_SESSION_ATTR_NAME);
            if (cmsUser == null) {
                throw new IllegalStateException("For Channel Manager preview requests there is expected to be a CMS user " +
                        "Session available");
            }

            if (compConfig.isExperiencePageComponent()) {
                if (requestContext.isRenderingHistory()) {
                    log.debug("Experience Page Component is not editable since a historic version is rendered");
                    return;
                }

                populatingAttributesMap.put(HST_EXPERIENCE_PAGE_COMPONENT, "true");
                // Check if no-one else is editing the draft
                final String handlePath = requestContext.getContentBean().getNode().getParent().getPath();
                // make sure to get the handle node VIA the correct cms user : the one via the requestContext is from a
                // jcr session combination of preview user + cms user session
                final Node handle = cmsUser.getNode(handlePath);
                if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    log.warn("Expected a handle node but found '{}' of type '{}'", handle.getPath(),
                            handle.getPrimaryNodeType().getName());
                    return;
                }
                final Workflow documentWorkflow = cmsUser.getWorkspace().getWorkflowManager().getWorkflow("default", handle);
                final Map<String, Serializable> hints = documentWorkflow.hints();
                if (FALSE.equals(hints.get(obtainEditableInstance().getAction()))) {
                    // Document most likely locked
                    final String inUseBy = (String) hints.get("inUseBy");
                    if (StringUtils.isNotBlank(inUseBy)) {
                        populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, inUseBy);
                    } else if (hints.get("requests") != null) {
                        populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, "workflow request");
                    } else {
                        populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, "unknown");
                    }
                }

            } else {
                populatingAttributesMap.put(HST_EXPERIENCE_PAGE_COMPONENT, "false");

                final Channel channel = requestContext.getResolvedMount().getMount().getChannel();

                if (channel != null && channel.isConfigurationLocked()) {
                    populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY, "system");
                    populatingAttributesMap.put(ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER, "false");

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
            }

        } catch (PathNotFoundException e) {
            // cms user cannot read component thus certainly cannot modify it
        } catch (RemoteException | WorkflowException | RepositoryException e) {
            log.error("Exception while checking access cmsUser session :", e);
        }

        if (compConfig.getXType() != null) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_XTYPE, compConfig.getXType());
        }


        populatingAttributesMap.put("uuid", compConfig.getCanonicalIdentifier());

        final String componentType = compConfig.getComponentType().toString();
        populatingAttributesMap.put(ChannelManagerConstants.HST_TYPE, componentType);
        populatingAttributesMap.put(ChannelManagerConstants.HST_SHARED, String.valueOf(compConfig.isShared()));

        populatingAttributesMap.put(ChannelManagerConstants.HST_LABEL, getLabel(compConfig));
        HstURLFactory urlFactory = requestContext.getURLFactory();
        HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, window.getReferenceNamespace(), null, requestContext);
        populatingAttributesMap.put("url", url.toString());
        populatingAttributesMap.put("refNS", window.getReferenceNamespace());

        if (compConfig.getLastModified() != null) {
            populatingAttributesMap.put(ChannelManagerConstants.HST_LAST_MODIFIED, String.valueOf(compConfig.getLastModified().getTimeInMillis()));
        }

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
