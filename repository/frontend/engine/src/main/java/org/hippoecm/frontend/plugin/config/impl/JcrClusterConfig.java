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
package org.hippoecm.frontend.plugin.config.impl;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.model.event.ListenerList;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.model.map.JcrValueList;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IClusterConfigListener;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrClusterConfig extends JcrPluginConfig implements IClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrClusterConfig.class);

    private static class Observable implements IObservable {
        private static final long serialVersionUID = 1L;

        private JcrClusterConfig config;
        private IObservationContext observationContext;
        private JcrEventListener listener;

        Observable(JcrClusterConfig config) {
            this.config = config;
        }

        public void setObservationContext(IObservationContext context) {
            this.observationContext = context;
        }

        public void startObservation() {
            String path = config.getNodeModel().getItemModel().getPath();
            int events = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                    | Event.PROPERTY_REMOVED;
            listener = new JcrEventListener(observationContext, events, path, true, null, null);
            listener.start();
        }

        public void stopObservation() {
            if (listener != null) {
                listener.stop();
                listener = null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Observable) {
                return ((Observable) obj).config.equals(config);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return config.hashCode() ^ 234457;
        }
    }

    static class EntryFilter implements Iterator<Entry<String, Object>> {

        Iterator<Entry<String, Object>> base;
        Entry<String, Object> next;

        EntryFilter(Iterator<Entry<String, Object>> base) {
            this.base = base;
        }

        void load() {
            if (next == null) {
                while (base.hasNext()) {
                    Entry<String, Object> candidate = base.next();
                    if (!(candidate.getValue() instanceof IPluginConfig)) {
                        next = candidate;
                        break;
                    }
                }
            }
        }

        public boolean hasNext() {
            load();
            return next != null;
        }

        public Entry<String, Object> next() {
            load();
            if (next != null) {
                Entry<String, Object> result = next;
                next = null;
                return result;
            }
            throw new IllegalArgumentException("No more entries in set");
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    };

    class PluginList extends AbstractList<IPluginConfig> implements Serializable {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id$";

        private static final long serialVersionUID = 1L;

        private List<String> plugins;

        PluginList() {
            plugins = loadPlugins();
        }

        List<String> loadPlugins() {
            List<String> plugins = new LinkedList<String>();
            try {
                NodeIterator nodes = getNode().getNodes();
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    if (node.isNodeType("frontend:plugin")) {
                        plugins.add(getPluginName(node));
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return plugins;
        }

        @Override
        public IPluginConfig get(int index) {
            return wrapConfig(getNode(index));
        }

        @Override
        public IPluginConfig set(int index, IPluginConfig element) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException("No such index");
            }
            IPluginConfig previous = remove(index);
            add(index, element);
            return previous;
        }

        @Override
        public int size() {
            return plugins.size();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void add(int index, IPluginConfig element) {
            if (index <= size()) {
                try {
                    Node node = getNode();
                    String name = element.getName();
                    if (name.indexOf('[') > 0) {
                        name = name.substring(0, name.indexOf('['));
                    }
                    Node child = node.addNode(name, "frontend:plugin");
                    JcrMap map = new JcrMap(new JcrNodeModel(child));
                    map.putAll(element);

                    if (node.getPrimaryNodeType().hasOrderableChildNodes() && (index < (size() - 1))) {
                        Node previous = getNode(index);
                        node.orderBefore(previous.getName() + "[" + previous.getIndex() + "]", child.getName() + "["
                                + child.getIndex() + "]");
                    }
                    if (getPluginContext() == null) {
                        plugins.add(index, getPluginName(child));
                        for (IClusterConfigListener listener : listeners) {
                            listener.onPluginAdded(wrapConfig(child));
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            } else {
                throw new IllegalArgumentException("Index too large");
            }
        }

        @Override
        public IPluginConfig remove(int index) {
            IPluginConfig result = new JavaPluginConfig(get(index));
            Node current = getNode(index);
            try {
                if (current != null) {
                    String name = getPluginName(current);
                    current.remove();
                    if (getPluginContext() == null) {
                        plugins.remove(name);
                        for (IClusterConfigListener listener : listeners) {
                            listener.onPluginRemoved(result);
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return result;
        }

        void process(Event event, List<IClusterConfigListener> listeners) {
            List<String> newPlugins = loadPlugins();
            Iterator<String> iter = plugins.iterator();
            while (iter.hasNext()) {
                String plugin = iter.next();
                if (!newPlugins.contains(plugin)) {
                    iter.remove();
                    for (IClusterConfigListener listener : listeners) {
                        // FIXME: return actual config
                        listener.onPluginRemoved(new JavaPluginConfig(plugin));
                    }
                }
            }
            for (String newPlugin : newPlugins) {
                if (!plugins.contains(newPlugin)) {
                    plugins.add(newPlugins.indexOf(newPlugin), newPlugin);
                    IPluginConfig config = get(newPlugins.indexOf(newPlugin));
                    for (IClusterConfigListener listener : listeners) {
                        listener.onPluginAdded(config);
                    }
                }
            }

            if (listeners.size() > 0) {
                if (event.getType() != 0) {
                    try {
                        String path = event.getPath();
                        path = path.substring(0, path.lastIndexOf('/'));
                        Node node = (Node) ((UserSession) Session.get()).getJcrSession().getItem(path);

                        Node root = getNode();
                        while (!node.isSame(root)) {
                            if (node.isNodeType("frontend:plugin")) {
                                IPluginConfig config = wrapConfig(node);
                                for (IClusterConfigListener listener : listeners) {
                                    listener.onPluginChanged(config);
                                }
                                break;
                            }
                            node = node.getParent();
                        }
                    } catch (RepositoryException ex) {
                        log.error("unable to find plugin configuration for event", ex);
                    }
                } else {
                    for (IClusterConfigListener listener : listeners) {
                        listener.onPluginChanged(null);
                    }
                }
            }
        }

        private Node getNode() {
            return getNodeModel().getNode();
        }

        private Node getNode(int index) {
            try {
                NodeIterator iter = getNode().getNodes();
                int idx = 0;
                while (iter.hasNext()) {
                    Node iterNode = iter.nextNode();
                    if (idx == index) {
                        return iterNode;
                    }
                    idx++;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return null;
        }

    }

    private PluginList configs;
    private List<IClusterConfigListener> listeners;

    public JcrClusterConfig(JcrNodeModel nodeModel) {
        this(nodeModel, null);
    }

    public JcrClusterConfig(JcrNodeModel nodeModel, IPluginContext context) {
        super(nodeModel, context);
        configs = new PluginList();
        listeners = new ListenerList<IClusterConfigListener>();
        if (context != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return new Observable(JcrClusterConfig.this);
                }

                public void onEvent(Iterator<? extends IEvent> iter) {
                    while (iter.hasNext()) {
                        configs.process(((JcrEvent) iter.next()).getEvent(), listeners);
                    }
                }

            }, IObserver.class.getName());
        }
    }

    public List<IPluginConfig> getPlugins() {
        return configs;
    }

    public List<String> getServices() {
        return getList("frontend:services");
    }

    public List<String> getReferences() {
        return getList("frontend:references");
    }

    public List<String> getProperties() {
        return getList("frontend:properties");
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> entries = super.entrySet();
        return new AbstractSet<Entry<String, Object>>() {

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new EntryFilter(entries.iterator());
            }

            @Override
            public int size() {
                EntryFilter filter = new EntryFilter(entries.iterator());
                int size = 0;
                while (filter.hasNext()) {
                    filter.next();
                    size++;
                }
                return size;
            }

        };
    }

    private List<String> getList(String key) {
        return new JcrValueList<String>(new JcrPropertyModel(getNodeModel().getItemModel().getPath() + "/" + key),
                PropertyType.STRING);
    }

    String getPluginName(Node node) throws RepositoryException {
        return wrapConfig(node).getName();
    }

    public void addClusterConfigListener(IClusterConfigListener listener) {
        listeners.add(listener);
    }

    public void removeClusterConfigListener(IClusterConfigListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JcrClusterConfig) {
            return super.equals(other);
        }
        return false;
    }

}
