/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

/**
 * <p>
 *      <code>PageUpdateEvent</code> which will be put on the internal Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link RuntimeException}
 *      through <code>PageUpdateEvent#setException(java.lang.RuntimeException)</code>. When a {@link RuntimeException} is
 *      set on this <code>PageUpdateEvent</code> by a listener, the
 *      {@link org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent} will rethrow the
 *      exception. The reason that this has to be done via this PageUpdateEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 *  <p>
 *     <strong>Note</strong> that listeners for <code>PageUpdateEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>PageUpdateEvent</code> to the guava event bus
 * </p>
 */
public interface PageUpdateEvent extends PageEvent<PageUpdateContext> {

}
