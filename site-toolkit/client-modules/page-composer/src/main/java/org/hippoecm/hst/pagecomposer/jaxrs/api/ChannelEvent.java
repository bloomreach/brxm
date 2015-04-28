/**
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.EventObject;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstRequestContext;


/**
 * ChannelEvent which is put on the internal HST Guava event bus for synchronous events dispatching where listeners to this
 * event can inject logic or short-circuit processing by throwing a {@link org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException}
 */
public class ChannelEvent extends EventObject {

    public enum ChannelEventType {
        PUBLISH,
        DISCARD
    }

    private final ChannelEventType channelEventType;
    private final List<String> userIds;
    private final HstRequestContext requestContext;
    private final Mount editingMount;
    private final HstSite editingPreviewSite;

    public ChannelEvent(final ChannelEventType channelEventType,
                        final List<String> userIds,
                        final Mount editingMount,
                        final HstSite editingPreviewSite,
                        final HstRequestContext requestContext) {
        super(requestContext);
        this.channelEventType = channelEventType;
        this.userIds = Collections.unmodifiableList(userIds);
        this.editingMount = editingMount;
        this.editingPreviewSite = editingPreviewSite;
        this.requestContext = requestContext;
    }

    public ChannelEventType getChannelEventType() {
        return channelEventType;
    }

    /**
     * @return unmodifiable list of users whose changes might be published / discarded / changed / affected
     */
    public List<String> getUserIds() {
        return userIds;
    }

    /**
     * @return the {@link HstRequestContext} that is used that triggers this this channel event
     */
    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @return the {@link Mount} that is being modified during this request
     */
    public Mount getEditingMount() {
        return editingMount;
    }

    /**
     * @return the preview {@link HstSite} that is being modified during this request
     */
    public HstSite getEditingPreviewSite() {
        return editingPreviewSite;
    }

    @Override
    public String toString() {
        return "ChannelEvent{" +
                "channelEventType=" + channelEventType +
                ", userIds=" + userIds +
                ", editingMount=" + editingMount +
                ", editingPreviewSite=" + editingPreviewSite +
                ", request=" + requestContext.getServletRequest() +
                '}';
    }
}
