/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.eventbus;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;

/**
 * Generic Hippo event bus.  Specific implementations can dispatch events based on metadata of the listener and event
 * objects.  (see e.g. the guava implementation)
 * <p>
 * Listeners can be registered using the whiteboard pattern.  Methods in the listener class hierarchy,
 * annotated with the {@link Subscribe} annotation will be invoked when events of a suitable type are dispatched.
 * (see the {@link Subscribe} documentation for details)
 *
 * Example :
 *
 * <pre>
 * <code>
 * //
 * MyObject() {
 *     ... initialization
 *
 *     // register as a listener using the whiteboard pattern
 *     HippoServiceRegistry.registerService(this, HippoEventBus.class);
 * }
 *
 * // implement a HippoEvent receiver method and annotate it with 'Subscribe'
 * // Note that the method with the annotation MUST be public
 * &#064;Subscribe
 * public void processHippoEvent(HippoEvent&lt;?&gt; event) {
 *     System.out.println(event);
 * }
 * </code>
 * </pre>
 *
 * If you are only interested in {@link org.onehippo.cms7.event.HippoEvent}s, you can implement the {@link HippoEventListener} instead. Then, you
 * also don't need to annotate the onEvent. For example:
 *
 *  <pre>
 * <code>
 * // get the eventBus
 * HippoServiceRegistry.registerService(new HippoEventListener() {
 *     // implement the onEvent method
 *     // Note that the method with the annotation MUST be public
 *     public void onEvent(HippoEvent&lt;?&gt; event) {
 *         System.out.println(event);
 *     }
 * }, HippoEventBus.class);
 * </code>
 *</pre>
 *
 */
@SingletonService
@WhiteboardService
public interface HippoEventBus {

    /**
     * @param listener
     */
    @Deprecated
    void register(Object listener);

    /**
     * Unregister the listener.
     *
     * @param listener
     *
     * @deprecated use the whiteboard pattern instead
     */
    @Deprecated
    void unregister(Object listener);

    /**
     * Publish an event to registered listeners.  Their annotated methods (with suitable parameter types)
     * will be invoked.
     *
     * @param event
     */
    void post(Object event);

}
