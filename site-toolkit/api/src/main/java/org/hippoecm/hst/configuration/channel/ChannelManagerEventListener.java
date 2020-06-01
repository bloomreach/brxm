/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.onehippo.cms7.services.hst.Channel;

/**
 * Allows implementers to register callback methods that will be executed
 * when a <CODE>ChannelManager</CODE> event occurs.
 * <p>
 * The channel change events are:
 * </p>
 * <ol>
 *   <li>creating a <CODE>Channel</CODE></li>
 *   <li>updating a <CODE>Channel</CODE></li>
 * </ol> 
 * 
 * @version $Id$
 */
public interface ChannelManagerEventListener {

    /**
     * Called immediately after a channel has been created through {@link ChannelManager#persist(Session, String, Channel)}.
     * <b>Note</b> that when for every ChannelManagerEventListener the {@link ChannelManagerEventListener#channelCreated(ChannelManagerEvent)} callback 
     * have been done, the JCR {@link Session} that belongs to {@link ChannelManagerEvent#getConfigRootNode()} is 
     * saved through {@link Session#save()} : Thus, any jcr modifications made on the backing jcr {@link Node} from the 
     * {@link ChannelManagerEvent#getConfigRootNode()} are being persisted. 
     * @param event the {@link ChannelManagerEvent}
     * @throws ChannelManagerEventListenerException an exception that an implementation may choose to throw to have the {@link ChannelManager} 
     * log a warning or even completely short circuit the channel processing when the {@link ChannelManagerEventListenerException} has
     * {@link ChannelManagerEventListenerException#getStatus()} equal to {@link Status#STOP_CHANNEL_PROCESSING}
     * 
     */
    void channelCreated(ChannelManagerEvent event) throws ChannelManagerEventListenerException;

    /**
     * Called immediately after a channel has been updated through {@link ChannelManager#save(Session, String, Channel)}.
     * <b>Note</b> that when for every ChannelManagerEventListener the {@link ChannelManagerEventListener#channelUpdated(ChannelManagerEvent)} callback 
     * method has been done, the JCR {@link Session} that belongs to {@link ChannelManagerEvent#getConfigRootNode()} is 
     * saved through {@link Session#save()} : Thus, any jcr modifications made on the backing jcr {@link Node} from the 
     * {@link ChannelManagerEvent#getConfigRootNode()} are being persisted.
     * @param event the {@link ChannelManagerEvent}
     * @throws ChannelManagerEventListenerException an exception that an implementation may choose to throw to have the {@link ChannelManager} 
     * log a warning or even completely short circuit the channel processing when the {@link ChannelManagerEventListenerException} has
     * {@link ChannelManagerEventListenerException#getStatus()} equal to {@link Status#STOP_CHANNEL_PROCESSING}
     */
    void channelUpdated(ChannelManagerEvent event) throws ChannelManagerEventListenerException;

}
