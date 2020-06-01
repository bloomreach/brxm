/**
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;


public class ChannelEventImpl extends BaseChannelEventImpl implements ChannelEvent {

    private static final long serialVersionUID = 1L;

    private final ChannelEventType channelEventType;
    private final List<String> userIds;
    private transient final HstRequestContext requestContext;
    private transient final Mount editingMount;
    private transient final HstSite editingPreviewSite;

    public ChannelEventImpl(final Channel channel,
                        final HstRequestContext requestContext,
                        final ChannelEventType channelEventType,
                        final List<String> userIds,
                        final Mount editingMount,
                        final HstSite editingPreviewSite) {
        super(channel);
        this.requestContext = requestContext;
        this.channelEventType = channelEventType;
        this.userIds = Collections.unmodifiableList(userIds);
        this.editingMount = editingMount;
        this.editingPreviewSite = editingPreviewSite;
    }

    @Override
    public ChannelEventType getChannelEventType() {
        return channelEventType;
    }

    /**
     * @return unmodifiable list of users whose changes might be published / discarded / changed / affected
     */
    @Override
    public List<String> getUserIds() {
        return userIds;
    }

    /**
     * @return the {@link HstRequestContext} that is used that triggers this this channel event
     */
    @Override
    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @return the {@link Mount} that is being modified during this request
     */
    @Override
    public Mount getEditingMount() {
        return editingMount;
    }

    /**
     * @return the preview {@link HstSite} that is being modified during this request. Note that in case of PREVIEW_CREATION
     * the returned {@link HstSite} is the <strong>live</strong> site because the preview site was not yet present during
     * the creation of the request context
     */
    @Override
    public HstSite getEditingPreviewSite() {
        return editingPreviewSite;
    }

    @Override
    public String toString() {
        return "ChannelEventImpl{" +
                "channelEventType=" + channelEventType +
                ", userIds=" + userIds +
                ", editingMount=" + editingMount +
                ", editingPreviewSite=" + editingPreviewSite +
                ", request=" + requestContext.getServletRequest() +
                ", exception=" + getException() +
                '}';
    }
}
