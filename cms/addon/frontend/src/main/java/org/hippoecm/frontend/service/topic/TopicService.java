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
package org.hippoecm.frontend.service.topic;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.service.ITopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicService implements ServiceListener, Serializable, ITopicService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private PluginContext context;
    private String topic;
    private List<ITopicService> peers;
    private List<MessageListener> listeners;

    public TopicService(String topic) {
        this.topic = topic;
        this.peers = new LinkedList<ITopicService>();
        this.listeners = new LinkedList<MessageListener>();
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

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public void processEvent(int type, String name, Serializable service) {
        switch (type) {
        case ServiceListener.ADDED:
            if (topic.equals(name) && (service instanceof ITopicService)) {
                if (service != this) {
                    peers.add((ITopicService) service);
                }
            } else {
                log.error("unknown service was added");
            }
            break;

        case ServiceListener.REMOVE:
            if (peers.contains(service)) {
                peers.remove(service);
            }
            break;
        }
    }

    public void publish(Message message) {
        Iterator<ITopicService> iter = peers.iterator();
        while (iter.hasNext()) {
            ITopicService peer = iter.next();
            peer.onPublish(message);
        }
    }

    public void onPublish(Message message) {
        for (Iterator<MessageListener> iter = listeners.iterator(); iter.hasNext();) {
            MessageListener listener = iter.next();
            listener.onMessage(message);
        }
    }
}
