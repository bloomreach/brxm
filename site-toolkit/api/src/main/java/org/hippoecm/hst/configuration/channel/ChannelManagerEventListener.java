/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelManagerEvent.ChannelManagerEventType;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;

/**
 * Allows implementers to register callback methods that will be executed when a {@link ChannelManagerEvent} occurs.
 * <p>
 * The channel manager events are defined in {@link ChannelManagerEventType}.
 * <p>
 */
public interface ChannelManagerEventListener {

    /**
     * Called immediately after a channel has been created or updated by <code>ChannelManager</code>.
     * The default implementation for this method is to dispatch the call to either {@link #channelCreated(ChannelManagerEvent)}
     * or {@link #channelUpdated(ChannelManagerEvent)} depending on {@link ChannelManagerEvent#getChannelManagerEventType()}.
     * <p>
     * <em>Note</em>: If you want to register a custom {@link ChannelManagerEventListener} class directly through
     * {@link ChannelManagerEventListenerRegistry#registerChannelManagerEventListener(ChannelManagerEventListener)},
     * not through configuring custom event listener beans in an overriding Spring Bean assembly resource,
     * then you must implement this method with Guava <code>@Subscribe</code> annotation explicitly. Otherwise,
     * the custom event listener will not be propagated. In that case, your custom event listener class may copy
     * the default logic of this default interface method or implement your own custom logic by checking {@link ChannelManagerEvent#getChannelManagerEventType()}
     * manually.
     * <p>
     * <em>Note</em>: When this event handler completes, the JCR {@link Session} that belongs to {@link ChannelManagerEvent#getConfigRootNode()}
     * is saved through {@link Session#save()}. Thus, any jcr modifications made on the backing jcr {@link Node}
     * from the {@link ChannelManagerEvent#getConfigRootNode()} are being persisted.
     * @param event the {@link ChannelManagerEvent}
     * @throws ChannelManagerEventListenerException an exception that an implementation may choose to throw to
     *         have the {@link ChannelManager} log a warning or even completely short circuit the channel processing
     *         when the {@link ChannelManagerEventListenerException} has {@link ChannelManagerEventListenerException#getStatus()}
     *         equal to {@link Status#STOP_CHANNEL_PROCESSING}
     */
    default void onChannelManagerEvent(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
        switch (event.getChannelManagerEventType()) {
        case CREATING:
            channelCreated(event);
            break;
        case UPDATING:
            channelUpdated(event);
            break;
        }
    }

    /**
     * Optional method for convenience, invoked by {@link #onChannelManagerEvent(ChannelManagerEvent)} by default
     * when {@link ChannelManagerEvent#getChannelManagerEventType()} is {@link ChannelManagerEventType#CREATING}.
     * <p>
     * <em>Note</em>: You may implement this method without having to override {@link #onChannelManagerEvent(ChannelManagerEvent)}
     * as {@link #onChannelManagerEvent(ChannelManagerEvent)} dispatches the call to this method if it's not overriden
     * and {@link ChannelManagerEvent#getChannelManagerEventType()} is {@link ChannelManagerEventType#CREATING}.
     */
    default void channelCreated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
    }

    /**
     * Optional method for convenience, invoked by {@link #onChannelManagerEvent(ChannelManagerEvent)} by default
     * when {@link ChannelManagerEvent#getChannelManagerEventType()} is {@link ChannelManagerEventType#UPDATING}.
     * <p>
     * <em>Note</em>: You may implement this method without having to override {@link #onChannelManagerEvent(ChannelManagerEvent)}
     * as {@link #onChannelManagerEvent(ChannelManagerEvent)} dispatches the call to this method if it's not overriden
     * and {@link ChannelManagerEvent#getChannelManagerEventType()} is {@link ChannelManagerEventType#UPDATING}.
     */
    default void channelUpdated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
    }

}
