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

/**
 * Channel Manager Event Listener Registry abstraction.
 */
public interface ChannelManagerEventListenerRegistry {

    /**
     * Register a {@link ChannelManagerEventListener}.
     * @param listener {@link ChannelManagerEventListener}
     */
    void registerChannelManagerEventListener(ChannelManagerEventListener listener);

    /**
     * Unregister a {@link ChannelManagerEventListener}.
     * @param listener {@link ChannelManagerEventListener}
     */
    void unregisterChannelManagerEventListener(ChannelManagerEventListener listener);

    /**
     * Register a channel event listener having Guava @Subscribe annotation.
     * @param listener a channel event listener having Guava @Subscribe annotation
     */
    void registerChannelEventListener(Object listener);

    /**
     * Unregister a channel event listener having Guava @Subscribe annotation.
     * @param listener a channel event listener having Guava @Subscribe annotation
     */
    void unregisterChannelEventListener(Object listener);

}
