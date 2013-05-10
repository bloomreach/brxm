/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.w3c.dom.Comment;

public class DefaultComponentWindowResponseAppender implements ComponentWindowResponseAppender {

    private List<ComponentWindowResponseAppender> subAppenders;

    public void setSubComposerComponentWindowResponseAppenders(List<ComponentWindowResponseAppender> subAppenders) {
        this.subAppenders = subAppenders;
    }

    @Override
    public void process(final HstComponentWindow rootWindow, final HstComponentWindow rootRenderingWindow, final HstComponentWindow window, final HstRequest request, final HstResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Boolean composerMode = (Boolean) session.getAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME);
        if (composerMode != null) {
            Mount mount = request.getRequestContext().getResolvedMount().getMount();
            // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
            HstComponentConfiguration compConfig  = ((HstComponentConfiguration)window.getComponentInfo());
            if (rootWindow == rootRenderingWindow && window == rootWindow) {
                response.addHeader(ChannelManagerConstants.HST_MOUNT_ID, mount.getIdentifier());
                response.addHeader(ChannelManagerConstants.HST_SITE_ID, mount.getHstSite().getCanonicalIdentifier());
                response.addHeader(ChannelManagerConstants.HST_PAGE_ID, compConfig.getCanonicalIdentifier());
                if (mount instanceof MutableMount) {
                    MutableMount mutableMount = (MutableMount)mount;
                    final String lockedBy = mutableMount.getLockedBy();
                    if (StringUtils.isNotBlank(lockedBy)) {
                        response.addHeader(ChannelManagerConstants.HST_MOUNT_LOCKED_BY, lockedBy);
                        response.addHeader(ChannelManagerConstants.HST_MOUNT_LOCKED_ON, String.valueOf(mutableMount.getLockedOn().getTimeInMillis()));
                    }
                }
                Object variant = session.getAttribute(ContainerConstants.RENDER_VARIANT);
                if (variant == null) {
                    variant = ContainerConstants.DEFAULT_PARAMETER_PREFIX;
                }
                response.addHeader(ChannelManagerConstants.HST_RENDER_VARIANT, variant.toString());
                response.addHeader(ChannelManagerConstants.HST_SITE_HAS_PREVIEW_CONFIG, String.valueOf(mount.getHstSite().hasPreviewConfiguration()));

            } else if(Boolean.TRUE.equals(composerMode)) {
                HashMap<String, String> attributes = new HashMap<String, String>();
                attributes.put("uuid", compConfig.getCanonicalIdentifier());
                if(compConfig.getXType() != null) {
                    attributes.put("xtype", compConfig.getXType());
                }
                if(compConfig.isInherited()) {
                    attributes.put("inherited", "true");
                }
                attributes.put("type", compConfig.getComponentType().toString());
                HstURLFactory urlFactory = request.getRequestContext().getURLFactory();
                HstURL url = urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE,
                        window.getReferenceNamespace(), null,
                        request.getRequestContext());
                attributes.put("url", url.toString());
                attributes.put("refNS", window.getReferenceNamespace());

                Comment comment = createCommentWithAttr(attributes, response);
                response.addPreamble(comment);
            }
        }

        for (ComponentWindowResponseAppender subAppender : subAppenders) {
            subAppender.process(rootWindow, rootRenderingWindow, window, request, response);
        }

    }

    private Comment createCommentWithAttr(HashMap<String, String> attributes, HstResponse response) {
        StringBuilder builder = new StringBuilder();
        for(Map.Entry<String, String> attr : attributes.entrySet()) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append("\"").append(attr.getKey()).append("\":").append("\"").append(attr.getValue()).append("\"");
        }
        Comment comment = response.createComment("{ " + builder.toString() +"}");
        return comment;
    }
}
