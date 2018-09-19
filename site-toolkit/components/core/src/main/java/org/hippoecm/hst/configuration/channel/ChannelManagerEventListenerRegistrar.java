/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.configuration.channel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for registering and unregistering {@link ChannelManagerEventListener} beans
 * defined in an hst site web application, through {@link ChannelManagerEventListenerRegistry}.
 */
public class ChannelManagerEventListenerRegistrar {

    private static Logger log = LoggerFactory.getLogger(ChannelManagerEventListenerRegistrar.class);

    private List<ChannelManagerEventListener> channelManagerEventListeners;

    public void setChannelManagerEventListeners(List<ChannelManagerEventListener> channelManagerEventListeners) {
        this.channelManagerEventListeners = new ArrayList<>();

        if (channelManagerEventListeners != null) {
            this.channelManagerEventListeners.addAll(channelManagerEventListeners);
        }
    }

    public void init() {
        if (CollectionUtils.isNotEmpty(channelManagerEventListeners)) {
            channelManagerEventListeners.forEach(listener -> {
                log.info("Registering channel manager event listener: {}", listener);
                ChannelManagerEventListenerRegistry.get().register(listener);
            });
        }
    }

    public void destroy() {
        if (CollectionUtils.isNotEmpty(channelManagerEventListeners)) {
            channelManagerEventListeners.forEach(listener -> {
                log.info("Unregistering channel manager event listener: {}", listener);
                ChannelManagerEventListenerRegistry.get().unregister(listener);
            });
        }
    }
}
