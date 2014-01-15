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

package org.onehippo.cms7.essentials.dashboard.event.listeners;


import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.onehippo.cms7.essentials.dashboard.event.PluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.ValidationEvent;

import com.google.common.eventbus.Subscribe;

/**
 * @version "$Id$"
 */
public class ValidationEventListener implements PluginEventListener<ValidationEvent> {

    public static final int MAX_ITEMS = 100;

    private final Queue<ValidationEvent> events = new ConcurrentLinkedQueue<>();

    @Override
    @Subscribe
    public void onPluginEvent(final ValidationEvent event) {
        if (events.size() == MAX_ITEMS) {
            events.poll();
        }
        events.add(event);
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }

    public int size() {
        return events.size();
    }

    public Queue<ValidationEvent> consumeEvents() {
        final Queue<ValidationEvent> validationEvents = new LinkedList<>(events);
        events.clear();
        return validationEvents;

    }
}
