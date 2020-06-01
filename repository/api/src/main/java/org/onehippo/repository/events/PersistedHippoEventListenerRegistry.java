/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.events;

import org.onehippo.cms7.services.WhiteboardProxiedServiceRegistry;

/**
 *  PersistedHippoEventListenerRegistry is singleton registry for decoupled/wiring lookup/wiring of
 *  {@link PersistedHippoEventListener}s using the <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">Whiteboard Pattern</a>.
 *  <p>
 *      For usage examples, see the {@link PersistedHippoEventListener} documentation.
 *  </p>
 */
public class PersistedHippoEventListenerRegistry extends WhiteboardProxiedServiceRegistry<PersistedHippoEventListener> {

    private static final PersistedHippoEventListenerRegistry INSTANCE = new PersistedHippoEventListenerRegistry();

    private PersistedHippoEventListenerRegistry() {
        super(PersistedHippoEventListener.class);
    }

    public static PersistedHippoEventListenerRegistry get() {
        return INSTANCE;
    }
}
