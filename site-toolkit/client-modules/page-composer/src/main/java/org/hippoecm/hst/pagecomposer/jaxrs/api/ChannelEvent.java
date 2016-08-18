/**
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 *      {@link ChannelEvent} which will be put on the internal HST Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link java.lang.RuntimeException}
 *      through {@link ChannelEvent#setException(java.lang.RuntimeException)}. When a {@link java.lang.RuntimeException} is
 *      set on this {@link ChannelEvent} by a listener, the
 *      {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will rethrow the
 *      exception. The reason that this has to be done via this ChannelEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 * <p>
 *     <strong>Note</strong> that listeners for {@link ChannelEvent}s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the {@link ChannelEvent} to the guava event bus
 * </p>
 */
public class ChannelEvent extends RuntimeExceptionEvent {

    private static final Logger log = LoggerFactory.getLogger(ChannelEvent.class);

    public enum ChannelEventType {
        PUBLISH,
        DISCARD,
        PREVIEW_CREATION
    }

    private final ChannelEventType channelEventType;
    private final List<String> userIds;
    private transient final HstRequestContext requestContext;
    private transient final Mount editingMount;
    private transient final HstSite editingPreviewSite;

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
     * @return the preview {@link HstSite} that is being modified during this request. Note that in case of PREVIEW_CREATION
     * the returned {@link HstSite} is the <strong>live</strong> site because the preview site was not yet present during
     * the creation of the request context
     */
    public HstSite getEditingPreviewSite() {
        return editingPreviewSite;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String toString() {
        return "ChannelEvent{" +
                "channelEventType=" + channelEventType +
                ", userIds=" + userIds +
                ", editingMount=" + editingMount +
                ", editingPreviewSite=" + editingPreviewSite +
                ", request=" + requestContext.getServletRequest() +
                ", exception=" + getException() +
                '}';
    }
}
