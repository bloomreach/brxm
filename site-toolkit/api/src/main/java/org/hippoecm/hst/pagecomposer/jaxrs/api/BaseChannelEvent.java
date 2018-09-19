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

import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;

/**
 * <p>
 *      <code>BaseChannelEvent</code> which will be put on the internal <code>ChannelEventBus</code> for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link java.lang.RuntimeException}
 *      through <code>RuntimeExceptionEvent#setException(java.lang.RuntimeException)</code>.
 *      When a {@link java.lang.RuntimeException} is set on this <code>BaseChannelEvent</code> by a listener, the
 *      {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will
 *      rethrow the exception. The reason that this has to be done via this <code>BaseChannelEvent</code> object
 *      is that the internal event bus always catches an exception thrown by a listener.
 * </p>
 * <p>
 *     <strong>Note</strong> that listeners for <code>ChannelEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>ChannelEvent</code> to the guava event bus
 * </p>
 */
public interface BaseChannelEvent extends RuntimeExceptionEvent {

    /**
     * Return the channel where this event occurs on.
     * @return the channel where this event occurs on
     */
    public Channel getChannel();

    public Logger getLogger();

}
