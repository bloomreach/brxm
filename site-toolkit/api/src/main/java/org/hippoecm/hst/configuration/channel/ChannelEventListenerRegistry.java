/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.channel;

import org.onehippo.cms7.services.WhiteboardServiceRegistry;

/**
 *  Singleton registry for decoupled/wring lookup/wiring of listeners that handle using
 *  the <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">Whiteboard Pattern</a>.
 */
public final class ChannelEventListenerRegistry extends WhiteboardServiceRegistry<Object> {

    private static final ChannelEventListenerRegistry INSTANCE = new ChannelEventListenerRegistry();

    private ChannelEventListenerRegistry() {
    }

    public static ChannelEventListenerRegistry get() {
        return INSTANCE;
    }

}
