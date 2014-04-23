/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.event.listeners;

import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.event.ValidationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @version "$Id$"
 */
public class ValidationEventListenerTest {

    private static Logger log = LoggerFactory.getLogger(ValidationEventListenerTest.class);
    private final EventBus bus = new EventBus();
    private final ValidationEventListener listener = new ValidationEventListener();

    @Before
    public void setUp() throws Exception {
        bus.register(listener);

    }

    @Test
    public void testOnPluginEvent() throws Exception {

        final int maxItems = ValidationEventListener.MAX_ITEMS + 10;
        for (int i = 0; i < maxItems; i++) {
            bus.post(new ValidationEvent(String.valueOf(i)));
        }
        Queue<ValidationEvent> pluginEvents = listener.consumeEvents();
        assertEquals(ValidationEventListener.MAX_ITEMS, pluginEvents.size());
        // above consume should remove all events:
        pluginEvents = listener.consumeEvents();
        assertEquals(0, pluginEvents.size());
        assertFalse(listener.hasEvents());
        assertEquals(0, listener.size());
        // add
        bus.post(new ValidationEvent("test"));
        pluginEvents = listener.consumeEvents();
        assertEquals(1, pluginEvents.size());
        assertEquals(0, listener.size());
        // consume again:
        pluginEvents = listener.consumeEvents();
        assertEquals(0, pluginEvents.size());
        assertEquals(0, listener.size());

    }

    @After
    public void tearDown() throws Exception {
        bus.unregister(listener);
    }
}

