/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;

/**
 * <p>
 *      <code>ChannelEvent</code> which will be put on the internal <code>ChannelManagerEventBus</code> for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link java.lang.RuntimeException}
 *      through <code>RuntimeExceptionEvent#setException(java.lang.RuntimeException)</code>.
 *      When a {@link java.lang.RuntimeException} is set on this <code>ChannelEvent</code> by a listener, the
 *      {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will
 *      rethrow the exception. The reason that this has to be done via this ChannelEvent object is that the internal
 *      Guava {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even
 *      when injecting a custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 * <p>
 *     <strong>Note</strong> that listeners for <code>ChannelEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>ChannelEvent</code> to the guava event bus
 * </p>
 */
public interface ChannelEvent extends RuntimeExceptionEvent {

    /**
     * Channel event type.
     */
    public enum ChannelEventType {
        PUBLISH,
        DISCARD,
        PREVIEW_CREATION
    }

    /**
     * Return the channel where this event occurs on.
     * @return the channel where this event occurs on
     */
    public Channel getChannel();

    /**
     * Return the channel event type.
     * @return the channel event type
     */
    public ChannelEventType getChannelEventType();

    /**
     * @return unmodifiable list of users whose changes might be published / discarded / changed / affected
     */
    public List<String> getUserIds();

    /**
     * @return the {@link HstRequestContext} that is used that triggers this this channel event
     */
    public HstRequestContext getRequestContext();

    /**
     * @return the {@link Mount} that is being modified during this request
     */
    public Mount getEditingMount();

    /**
     * @return the preview {@link HstSite} that is being modified during this request. Note that in case of PREVIEW_CREATION
     * the returned {@link HstSite} is the <strong>live</strong> site because the preview site was not yet present during
     * the creation of the request context
     */
    public HstSite getEditingPreviewSite();

}
