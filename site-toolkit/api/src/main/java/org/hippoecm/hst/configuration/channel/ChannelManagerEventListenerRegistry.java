/*
 *  Copyright 2018-2023 Bloomreach
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

import org.onehippo.cms7.services.WhiteboardProxiedServiceRegistry;

/**
 *  Singleton registry for decoupled/wring lookup/wiring of listeners to the {@link ChannelManagerEvent}s using
 *  the <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">Whiteboard Pattern</a>.
 *  <p>
 *  HST site webapp may want to subscribe {@link ChannelManagerEvent}s from the Channel Manager.
 *  In that case, you should implement a {@link ChannelManagerEventListener} and register a listener instance
 *  through <code>ChannelManagerEventListenerRegistry.register(listener);</code>.
 *  <p>
 *  <em>Note:</em> Also, you should unregister the listener when the listener is not needed any more (e.g. your
 *  site webapp getting stopped or undeployed) through <code>ChannelManagerEventListenerRegistry.unregister(listener);</code>
 *  <p>
 */
public final class ChannelManagerEventListenerRegistry extends WhiteboardProxiedServiceRegistry<ChannelManagerEventListener> {

    private static final ChannelManagerEventListenerRegistry INSTANCE = new ChannelManagerEventListenerRegistry();

    private ChannelManagerEventListenerRegistry() {
        super(ChannelManagerEventListener.class);
    }

    public static ChannelManagerEventListenerRegistry get() {
        return INSTANCE;
    }

}
