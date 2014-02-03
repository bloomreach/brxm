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


import javax.inject.Singleton;

import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.event.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

/**
 * @version "$Id$"
 */
@Component
@Singleton
public class InstructionsEventListener implements PluginEventListener<InstructionEvent> {

    private static final Logger log = LoggerFactory.getLogger(InstructionsEventListener.class);
    private transient int counter;

    @Override
    @Subscribe
    public void onPluginEvent(final InstructionEvent event) {
        log.info("INSTRUCTION EVENT: {}", event.getMessage());
        counter++;
    }

    // TODO remove, debugging only
    public int getNrInstructions() {
        return counter;
    }

    public void reset() {
        counter = 0;
    }
}
