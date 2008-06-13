/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.legacy.plugin.channel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.IClusterable;

import org.hippoecm.frontend.legacy.model.IPluginModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class Channel implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static int ADDED = 1;
    private static int REMOVED = 2;

    static final Logger log = LoggerFactory.getLogger(Channel.class);

    private ChannelFactory factory;

    private List<INotificationListener> listeners;
    private List<ListChange<INotificationListener>> listenerChanges;
    private int listenersInUse;

    private List<ListChange<IRequestHandler>> handlerChanges;
    private List<IRequestHandler> handlers;
    private int handlersInUse;

    public Channel(ChannelFactory factory) {
        this.factory = factory;

        listeners = new LinkedList<INotificationListener>();
        listenerChanges = new LinkedList<ListChange<INotificationListener>>();
        listenersInUse = 0;

        handlers = new LinkedList<IRequestHandler>();
        handlerChanges = new LinkedList<ListChange<IRequestHandler>>();
        handlersInUse = 0;
    }

    public void subscribe(INotificationListener listener) {
        if (listenersInUse > 0) {
            listenerChanges.add(new ListChange<INotificationListener>(listener, ADDED));
        } else {
            listeners.add(listener);
        }
    }

    public void unsubscribe(INotificationListener listener) {
        if (listenersInUse > 0) {
            listenerChanges.add(new ListChange<INotificationListener>(listener, REMOVED));
        } else {
            listeners.remove(listener);
        }
    }

    public void register(IRequestHandler handler) {
        if (handlersInUse > 0) {
            handlerChanges.add(new ListChange<IRequestHandler>(handler, ADDED));
        } else {
            handlers.add(handler);
        }
    }

    public void unregister(IRequestHandler handler) {
        if (handlersInUse > 0) {
            handlerChanges.add(new ListChange<IRequestHandler>(handler, REMOVED));
        } else {
            handlers.remove(handler);
        }
    }

    public ChannelFactory getMessageFactory() {
        return factory;
    }

    public void publish(Notification notification) {
        listenersInUse++;
        Iterator<INotificationListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            INotificationListener listener = iter.next();
            listener.receive(notification);
        }
        if (--listenersInUse == 0) {
            Iterator<ListChange<INotificationListener>> changeIter = listenerChanges.iterator();
            while (changeIter.hasNext()) {
                ListChange<INotificationListener> change = changeIter.next();
                if (change.operation == ADDED) {
                    listeners.add(change.object);
                } else {
                    listeners.remove(change.object);
                }
                changeIter.remove();
            }
        }
    }

    public void send(Request request) {
        handlersInUse++;
        Iterator<IRequestHandler> iter = handlers.iterator();
        while (iter.hasNext()) {
            IRequestHandler handler = iter.next();
            handler.handle(request);
        }
        if (--handlersInUse == 0) {
            Iterator<ListChange<IRequestHandler>> changeIter = handlerChanges.iterator();
            while (changeIter.hasNext()) {
                ListChange<IRequestHandler> change = changeIter.next();
                if (change.operation == ADDED) {
                    handlers.add(change.object);
                } else {
                    handlers.remove(change.object);
                }
                changeIter.remove();
            }
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

    class ListChange<E> implements IClusterable {
        private static final long serialVersionUID = 1L;

        int operation;
        E object;

        ListChange(E object, int operation) {
            this.object = object;
            this.operation = operation;
        }
    }
}
