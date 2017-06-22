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

import org.onehippo.cms7.services.hst.Channel;

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
     */
    Blueprint getBlueprint();

    /**
     * @deprecated since 3.2.0 (CMS 10.2.0). Use {@link Channel#getId() getChannel().getId()} instead
     */
    @Deprecated
    String getChannelId();

    /**
     * @return the {@link Channel} which was used as an input during channel creation or update. This will never return
     * <code>null</code>
     */
    Channel getChannel();

    /**
     * @return the {@link Node} for /hst:hst. This will never return <code>null</code>
     */
    Node getConfigRootNode();

}
