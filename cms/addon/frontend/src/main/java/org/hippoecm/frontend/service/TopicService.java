/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.service;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicService implements ServiceListener, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private PluginContext context;
    private String topic;
    private List<TopicService> peers;
    private MessageListener listener;

    public TopicService(String topic, MessageListener listener) {
        this.topic = topic;
        this.peers = new LinkedList<TopicService>();
        this.listener = listener;
    }

    public void init(PluginContext context) {
        this.context = context;
        context.registerListener(this, topic);
        context.registerService(this, topic);
    }

    public void destroy() {
        context.unregisterService(this, topic);
        context.unregisterListener(this, topic);
    }

    public void processEvent(int type, String name, Serializable service) {
        switch (type) {
        case ServiceListener.ADDED:
            if (topic.equals(name) && (service instanceof TopicService)) {
                if (service != this) {
                    peers.add((TopicService) service);
                }
            } else {
                log.error("unknown service was added");
            }
            break;

        case ServiceListener.REMOVED:
            if (peers.contains(service)) {
                peers.remove(service);
            }
            break;
        }
    }

    public void publish(Message message) {
        Iterator<TopicService> iter = peers.iterator();
        while (iter.hasNext()) {
            TopicService peer = iter.next();
            peer.onPublish(message);
        }
    }

    public void onPublish(Message message) {
        listener.onMessage(message);
    }
}
