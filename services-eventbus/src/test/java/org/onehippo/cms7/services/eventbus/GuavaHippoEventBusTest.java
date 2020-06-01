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

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GuavaHippoEventBusTest {

    public class Listener {
        boolean fired = false;

        @Subscribe
        public void eventFired(Object payload) {
            fired = true;
            synchronized (GuavaHippoEventBusTest.this) {
                GuavaHippoEventBusTest.this.notify();
            }
        }
    }

    @Test
    public void testEventBusWithWhiteboardServiceTracker() throws InterruptedException {
        HippoEventBus eventBus = new GuavaHippoEventBus();
        Listener listener = new Listener();
        HippoEventListenerRegistry.get().register(listener);
        eventBus.post(new Object());
        synchronized (this) {
            wait(500);
        }
        assertTrue(listener.fired);

        listener.fired = false;
        HippoEventListenerRegistry.get().unregister(listener);
        eventBus.post(new Object());
        synchronized (this) {
            wait(500);
        }
        assertFalse(listener.fired);
    }

}
