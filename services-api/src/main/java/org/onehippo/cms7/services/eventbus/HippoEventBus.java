/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.event.HippoEvent;

/**
 * Generic Hippo event bus. Specific implementations can dispatch events based on metadata of the listener and event
 * objects.  (see e.g. the guava implementation)
 * <p>
 *     Listeners must be registered through the {@link HippoEventListenerRegistry} which the HippoEventBus service
 *     will (must) query, or track itself, for the available listeners to dispatch events to (Whiteboard pattern).
 * </p>
 * <p>
 *     Methods in the listener class hierarchy annotated with the {@link Subscribe} annotation will be invoked when
 *     events of a suitable type are dispatched. (see the {@link Subscribe} documentation for details). Example:
 * </p>
 * <p>
 *     Example listener implementation using the {@link Subscribe} annotation:
 *     <pre><code>
 *     MyObject() {
 *         ... initialization
 *
 *         // register as a listener using the HippoEventListenerRegistry:
 *         HippoEventListenerRegistry.get().register(this);
 *
 *         // implement a HippoEvent receiver method and annotate it with 'Subscribe'
 *         // Note that the method with the annotation MUST be public
 *         &#064;Subscribe
 *         public void processHippoEvent(HippoEvent&lt;?&gt; event) {
 *             System.out.println(event);
 *         }
 *     }
 *
 *     // get the eventBus
 *     HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
 *     // post an event which subsequently will be printed by the &#064;Subscribe annotated method defined above
 *     eventBus.post(new HippoEvent("foo").message("bar"));
 *     </code></pre>
 * </p>
 * <p>
 *     When you are only interested in {@link org.onehippo.cms7.event.HippoEvent}s, you also can implement the
 *     {@link HippoEventListener} instead. Then you also don't need to annotate the onEvent method. For example:
 *     <pre><code>
 *     HippoEventListenerRegistry.get().register(new HippoEventListener() {
 *         // implement the onEvent method
 *         // Note that the method MUST be public
 *         public void onEvent(HippoEvent&lt;?&gt; event) {
 *             System.out.println(event);
 *         }
 *     });</code></pre>
 * </p>
 */
public interface HippoEventBus {

    /**
     * Publish an event to registered listeners. Their {@link Subscribe} annotated methods (with suitable parameter
     * types), or {@Link HippoEventListener} implementations their {@link HippoEventListener#onEvent(HippoEvent)} method
     * will be invoked.
     * @param event event to be published
     */
    void post(Object event);

}
