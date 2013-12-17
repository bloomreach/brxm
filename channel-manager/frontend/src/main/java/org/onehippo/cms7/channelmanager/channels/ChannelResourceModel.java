/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channels;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChannelResourceModel extends LoadableDetachableModel<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChannelResourceModel.class);

    private final Channel channel;
    private final String key;
    private final ChannelStore channelStore;

    ChannelResourceModel(String key, Channel channel, ChannelStore channelStore) {
        this.channel = channel;
        this.key = key;
        this.channelStore = channelStore;
    }

    @Override
    protected String load() {
        try {
            if (StringUtils.isNotEmpty(key)) {
                Properties bundleProperties = channelStore.getChannelResourceValues(channel);
                if (bundleProperties != null && bundleProperties.containsKey(key)) {
                    return bundleProperties.getProperty(key);
                }
            }
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Could not resolve values for channel with id '" + channel.getId() + "'", ce);
            } else {
                log.warn("Could not resolve values for channel with id '{}' - {}", channel.getId(), ce.toString());
            }
        }

        return null;
    }

}