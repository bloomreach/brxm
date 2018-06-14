/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstEventsDispatcher {

    private static final Logger log = LoggerFactory.getLogger(HstEventsDispatcher.class);

    private final HstEventsCollector hstEventsCollector;

    private final List<HstEventConsumer> hstEventConsumers;

    public HstEventsDispatcher(final HstEventsCollector hstEventsCollector, final List<HstEventConsumer> hstEventConsumers) {
        this.hstEventsCollector = hstEventsCollector;
        this.hstEventConsumers = hstEventConsumers;
    }

    // meant for unit test only
    HstEventsCollector getHstEventsCollector() {
        return hstEventsCollector;
    }

    public void dispatchHstEvents() {
        long start = System.currentTimeMillis();
        Set<HstEvent> events = hstEventsCollector.getAndClearEvents();
        for (HstEventConsumer consumer : hstEventConsumers) {
            consumer.handleEvents(events);
        }
        log.info("Dispatching '{}' events took '{}' ms.", events.size(), (System.currentTimeMillis() - start));
    }
}
