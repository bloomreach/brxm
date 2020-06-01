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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *      {@link PageCopyEvent} which will be put on the internal HST Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link java.lang.RuntimeException}
 *      through {@link PageCopyEvent#setException(java.lang.RuntimeException)}. When a {@link java.lang.RuntimeException} is
 *      set on this {@link PageCopyEvent} by a listener, the
 *      {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will rethrow the
 *      exception. The reason that this has to be done via this PageCopyEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 *  <p>
 *     <strong>Note</strong> that listeners for {@link PageCopyEvent}s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the {@link PageCopyEvent} to the guava event bus
 * </p>
 */
public class PageCopyEvent extends RuntimeExceptionEvent {

    private static final Logger log = LoggerFactory.getLogger(PageCopyEvent.class);

    public PageCopyEvent(final PageCopyContext pageCopyContext) {
        super(pageCopyContext);
    }

    public PageCopyContext getPageCopyContext() {
        return (PageCopyContext)getSource();
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String toString() {
        return "CopyPageEvent{" +
                "pageCopyContext=" + getPageCopyContext() +
                "exception=" + getException() +
                '}';
    }
}
