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
 * Developers can implement this {@link HippoEventListener} interface and implement {@link #onEvent(HippoEvent)} to 
 * get a callback whenever a {@link HippoEvent} is posted to the {@link HippoEventBus} by {@link HippoEventBus#post(Object)} 
 * <p>
 *     Note that this is a convenience interface; it is not necessary to use, as any method that is annotated with the
 *     {@link Subscribe} annotation having a HippoEvent argument type, will be invoked.
 * </p>
 * <p>Example code:
 * <pre><code>
 *     HippoEventListenerRegistry.get().register(new HippoEventListener() {
 *         // implement the onEvent method
 *         // Note that the method with the annotation MUST be public
 *         public void onEvent(HippoEvent<?> event) {
 *             System.out.println(event);
 *     }
 *
 *     // get the eventBus
 *     HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
 *     // post an event which subsequently will be printed by the listener defined above
 *     eventBus.post(new HippoEvent("foo").message("bar"));
 * }); 
 *  </pre>
 */
public interface HippoEventListener {

    /**
     * @param event the {@link HippoEvent}
     */
    @Subscribe
    void onEvent(HippoEvent<?> event);
}
