/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugin.channel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.IPluginModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Channel implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Channel.class);

    private List<INotificationListener> listeners;
    private List<IRequestHandler> handlers;
    private ChannelFactory factory;

    public Channel(ChannelFactory factory) {
        this.factory = factory;
        handlers = new LinkedList<IRequestHandler>();
        listeners = new LinkedList<INotificationListener>();
    }

    public void subscribe(INotificationListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(INotificationListener listener) {
        listeners.remove(listener);
    }

    public void register(IRequestHandler handler) {
        handlers.add(handler);
    }

    public void unregister(IRequestHandler handler) {
        handlers.remove(handler);
    }

    public ChannelFactory getMessageFactory() {
        return factory;
    }

    public void publish(Notification notification) {
        Iterator<INotificationListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            INotificationListener listener = iter.next();
            listener.receive(notification);
        }
    }

    public void send(Request request) {
        Iterator<IRequestHandler> iter = handlers.iterator();
        while (iter.hasNext()) {
            IRequestHandler handler = iter.next();
            handler.handle(request);
        }
    }

    public Request createRequest(String operation, IPluginModel model) {
        return new Request(operation, model);
    }

    public Notification createNotification(String operation, IPluginModel model) {
        return new Notification(operation, model);
    }

    public Notification createNotification(Request request) {
        return new Notification(request);
    }
}
