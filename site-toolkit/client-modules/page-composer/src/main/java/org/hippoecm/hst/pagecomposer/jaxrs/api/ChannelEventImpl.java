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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChannelEventImpl implements ChannelEvent {

    private static final Logger log = LoggerFactory.getLogger(ChannelEventImpl.class);

    private final Channel channel;
    private transient final HstRequestContext requestContext;
    private final ChannelEventType channelEventType;

    private List<String> userIds;
    private transient Mount editingMount;
    private transient HstSite editingPreviewSite;
    private transient RuntimeException exception;

    public ChannelEventImpl(final Channel channel, final HstRequestContext requestContext) {
        this(channel, requestContext, null);
    }

    public ChannelEventImpl(final Channel channel, final HstRequestContext requestContext, final ChannelEventType channelEventType) {
        this.channel = channel;
        this.requestContext = requestContext;
        this.channelEventType = channelEventType;
    }

    @Override
    public Channel getChannel() {
        return null;
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
        if (userIds == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(userIds);
    }

    public void setUserIds(List<String> userIds) {
        if (userIds == null) {
            this.userIds = new ArrayList<>();
        } else {
            this.userIds = new ArrayList<>(userIds);
        }
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

    public void setEditingMount(Mount editingMount) {
        this.editingMount = editingMount;
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

    public void setEditingPreviewSite(HstSite editingPreviewSite) {
        this.editingPreviewSite = editingPreviewSite;
    }

    @Override
    public RuntimeException getException() {
        return exception;
    }

    @Override
    public void setException(RuntimeException runtimeException) {
        this.exception = runtimeException;
    }

    public Logger getLogger() {
        return log;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "channel=" + channel +
                ", channelEventType=" + channelEventType +
                ", userIds=" + userIds +
                ", editingMount=" + editingMount +
                ", editingPreviewSite=" + editingPreviewSite +
                ", request=" + requestContext.getServletRequest() +
                ", exception=" + getException() +
                '}';
    }
}
