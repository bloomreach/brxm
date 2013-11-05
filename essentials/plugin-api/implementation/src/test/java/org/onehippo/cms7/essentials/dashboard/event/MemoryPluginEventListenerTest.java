/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.event;

import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;

import com.google.common.eventbus.EventBus;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class MemoryPluginEventListenerTest {


    private final EventBus bus  = new EventBus();
    private final MemoryPluginEventListener listener  = new MemoryPluginEventListener();

    @Before
    public void setUp() throws Exception {
        bus.register(listener);

    }

    @Test
    public void testOnPluginEvent() throws Exception {

        final int maxItems = MemoryPluginEventListener.MAX_ITEMS + 10;
        for (int i = 0; i < maxItems; i++) {
             bus.post(new DisplayEvent(String.valueOf(i)));
        }
        Queue<DisplayEvent> pluginEvents = listener.consumeEvents();
        assertEquals(MemoryPluginEventListener.MAX_ITEMS, pluginEvents.size());
        // above consume should remove all events:
        pluginEvents = listener.consumeEvents();
        assertEquals(0, pluginEvents.size());
        // add
        bus.post(new DisplayEvent("test"));
        pluginEvents = listener.consumeEvents();
        assertEquals(1, pluginEvents.size());
        // consume again:
        pluginEvents = listener.consumeEvents();
        assertEquals(0, pluginEvents.size());

    }

    @After
    public void tearDown() throws Exception {
        bus.unregister(listener);
    }
}
