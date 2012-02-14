/*
 *  Copyright 2012 Hippo.
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
     * Called immediately after a channel has been created through {@link ChannelManager#persist(String, Channel)}.
     * @param event
     */
    void channelCreated(ChannelManagerEvent event);

    /**
     * Called immediately after a channel has been updated through {@link ChannelManager#save(Channel)}.
     * @param event
     */
    void channelUpdated(ChannelManagerEvent event);

}
