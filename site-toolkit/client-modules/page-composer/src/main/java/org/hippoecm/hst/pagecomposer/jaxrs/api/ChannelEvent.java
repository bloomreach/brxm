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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent} which is put on the internal HST Guava event bus for
 * <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 * processing by setting a {@link java.lang.RuntimeException}
 * through {@link org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent#setException(java.lang.RuntimeException)}. When a
 * {@link java.lang.RuntimeException} is set on this
 * {@link org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent} by a listener, the
 * {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will rethrow the
 * exception. The reason that this has to be done via this ChannelEvent object is that Guava
 * {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 * custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 */
public class ChannelEvent extends EventObject {

    private static final Logger log = LoggerFactory.getLogger(ChannelEvent.class);

    public enum ChannelEventType {
        PUBLISH,
        DISCARD
    }

    private final ChannelEventType channelEventType;
    private final List<String> userIds;
    private transient final HstRequestContext requestContext;
    private transient final Mount editingMount;
    private transient final HstSite editingPreviewSite;
    private transient RuntimeException exception;

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

    public RuntimeException getException() {
        return exception;
    }

    public void setException(final RuntimeException exception) {
        if (this.exception != null) {
            log.debug("Skipping exception '{}' for ChannelEvent {} because already an exception set.", exception.toString(),
                    this);
            return;
        }
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "ChannelEvent{" +
                "channelEventType=" + channelEventType +
                ", userIds=" + userIds +
                ", editingMount=" + editingMount +
                ", editingPreviewSite=" + editingPreviewSite +
                ", request=" + requestContext.getServletRequest() +
                ", exception=" + exception +
                '}';
    }
}
