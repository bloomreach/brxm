/*
 *  Copyright 2011 Hippo.
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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintService implements Blueprint {

    final static Logger log = LoggerFactory.getLogger(BlueprintService.class);

    private final String id;
    private final String name;
    private final String description;
    private final String path;

    private final Channel prototypeChannel;

    public BlueprintService(final Node bluePrint) throws RepositoryException {
        path = bluePrint.getPath();

        id = bluePrint.getName();

        if (bluePrint.hasProperty("hst:name")) {
            this.name = bluePrint.getProperty("hst:name").getString();
        } else {
            this.name = this.id;
        }

        if (bluePrint.hasProperty("hst:description")) {
            this.description = bluePrint.getProperty("hst:description").getString();
        } else {
            this.description = null;
        }

        if (bluePrint.hasNode(HstNodeTypes.NODENAME_HST_CHANNEL)) {
            this.prototypeChannel = ChannelPropertyMapper.readChannel(bluePrint.getNode(HstNodeTypes.NODENAME_HST_CHANNEL));
        } else {
            this.prototypeChannel = new Channel(null);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public Channel createChannel(String channelId) {
        Channel channel = new Channel(channelId);
        channel.setName(channelId);
        channel.setChannelInfoClass(prototypeChannel.getChannelInfoClass());

        Map<String, Object> properties = channel.getProperties();

        Channel prototype = getPrototypeChannel();
        channel.setChannelInfoClass(prototype.getChannelInfoClass());
        properties.putAll(prototype.getProperties());

        return channel;
    }

    public Channel getPrototypeChannel() {
        return prototypeChannel;
    }

    public Node getNode(final Session session) throws RepositoryException {
        return session.getNode(path);
    }

}
