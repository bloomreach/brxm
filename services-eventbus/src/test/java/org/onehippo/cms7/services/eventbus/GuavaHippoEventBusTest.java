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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GuavaHippoEventBusTest {

    public class Listener {

        volatile boolean fired = false;

        CountDownLatch latch = new CountDownLatch(1);

        @Subscribe
        public void eventFired(Object payload) {
            fired = true;
            latch.countDown();
        }

        public void reset() {
            fired = false;
            latch = new CountDownLatch(1);
        }
    }

    @Test
    public void testEventBusWithWhiteboardServiceTracker() throws InterruptedException {
        HippoEventBus eventBus = new GuavaHippoEventBus();
        Listener listener = new Listener();

        HippoEventListenerRegistry.get().register(listener);
        eventBus.post(new Object());

        listener.latch.await(250, TimeUnit.MILLISECONDS);

        assertTrue(listener.fired);

        listener.reset();

        HippoEventListenerRegistry.get().unregister(listener);
        eventBus.post(new Object());

        listener.latch.await(250, TimeUnit.MILLISECONDS);
        assertFalse(listener.fired);
    }

    @Test
    public void testObjectOfSameClassAfterUnregister() throws InterruptedException {
        // initialize
        HippoEventBus eventBus = new GuavaHippoEventBus();
        Listener listener = new Listener();
        Listener listener2 = new Listener();
        HippoEventListenerRegistry.get().register(listener);
        HippoEventListenerRegistry.get().register(listener2);
        eventBus.post(new Object());

        assertTrue(listener.latch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(listener2.latch.await(250, TimeUnit.MILLISECONDS));

        // verify both work
        assertTrue(listener.fired);
        assertTrue(listener2.fired);

        //reset
        listener.reset();
        listener2.reset();

        // unregister one
        HippoEventListenerRegistry.get().unregister(listener);
        eventBus.post(new Object());

        assertFalse(listener.latch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(listener2.latch.await(250, TimeUnit.MILLISECONDS));

        // verify the unregistered one didn't fire, but the other did
        assertFalse(listener.fired);
        assertTrue(listener2.fired);

        //reset
        listener2.reset();
        HippoEventListenerRegistry.get().unregister(listener2);

    }

}
