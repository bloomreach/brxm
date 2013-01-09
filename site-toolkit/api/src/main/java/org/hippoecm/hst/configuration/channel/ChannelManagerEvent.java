/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * This event type is used by the ChannelManagerEventListener 
 * in order to identify the channel and its related information.
 * 
 * @version $Id$
 */
public interface ChannelManagerEvent {

    /**
     * Returns a blueprint object which is being used during the channel creation.
     * If the event is not triggered on channel creation, it returns null.
     * @return
     */
    Blueprint getBlueprint();

    /**
     * Returns the ID of the channel which is being created or updated.
     * If a channel is created, you must use this method to get the ID of the created channel.
     * Otherwise, this method will return the same value as <CODE>getChannel().getId()</CODE>.
     * @return
     */
    String getChannelId();

    /**
     * Returns the channel which was used as an input during channel creation or update.
     * @return
     */
    Channel getChannel();

    /**
     * Returns the HST Configuration root node. e.g., /hst:hst
     * @return
     */
    Node getConfigRootNode();

}
