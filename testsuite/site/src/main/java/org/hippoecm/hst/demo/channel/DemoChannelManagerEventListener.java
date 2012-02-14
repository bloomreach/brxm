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
package org.hippoecm.hst.demo.channel;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelManagerEvent;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DemoChannelManagerEventListener
 * <P>
 * Example <CODE>ChannelManagerEventListener</CODE> implementation
 * which simply logs all the triggered event information.
 * </P>
 * 
 * @version $Id$
 */
public class DemoChannelManagerEventListener implements ChannelManagerEventListener {
    
    private static Logger log = LoggerFactory.getLogger(DemoChannelManagerEventListener.class);

    public void channelCreated(ChannelManagerEvent event) {
        log.info("A channel has been created. {}", channelManagerEventToString(event));
    }

    public void channelUpdated(ChannelManagerEvent event) {
        log.info("A channel has been updated. {}", channelManagerEventToString(event));
    }

    private String channelManagerEventToString(ChannelManagerEvent event) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("{ ");
        
        Blueprint blueprint = event.getBlueprint();
        
        if (blueprint != null) {
            sb.append("blueprint: [ ");
            sb.append(blueprint.getId()).append(", ");
            sb.append(blueprint.getName()).append(", ");
            sb.append(blueprint.getDescription());
            sb.append(" ], ");
        }
        
        Channel channel = event.getChannel();
        
        if (channel != null) {
            sb.append("channel: [ ");
            sb.append(event.getChannelId()).append(", ");
            sb.append(channel.getName()).append(", ");
            sb.append(channel.getContentRoot());
            sb.append(" ], ");
        }
        
        Node configRootNode = event.getConfigRootNode();

        try {
            if (configRootNode != null) {
                sb.append("configRootNode: ");
                sb.append(configRootNode.getPath());
            }
        } catch (RepositoryException e) {
            log.error("Failed to read channel node path", e);
        }

        sb.append(" }");
        return sb.toString();
    }
}
