/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.model.map.JcrValueList;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrClusterConfig extends JcrPluginConfig implements IClusterConfig {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrClusterConfig.class);

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
                    if (node.isNodeType(FrontendNodeType.NT_PLUGIN)) {
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

        @Override
        public void add(int index, IPluginConfig element) {
            if (index <= size()) {
                try {
                    Node node = getNode();
                    String name = element.getName();
                    if (name.indexOf('[') > 0) {
                        name = name.substring(0, name.indexOf('['));
                    }
                    Node child = node.addNode(name, FrontendNodeType.NT_PLUGIN);
                    JcrMap map = new JcrMap(new JcrNodeModel(child));
                    for (Map.Entry<String, Object> entry : element.entrySet()) {
                        Object value = unwrap(entry.getValue());
                        map.put(entry.getKey(), value);
                    }

                    if (node.getPrimaryNodeType().hasOrderableChildNodes() && (index < (size() - 1))) {
                        Node previous = getNode(index);
                        node.orderBefore(child.getName() + (child.getIndex() > 1 ? "[" + child.getIndex() + "]" : ""),
                                previous.getName() + (previous.getIndex() > 1 ? "[" + previous.getIndex() + "]" : ""));
                    }

                    notifyObservers();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
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
                    current.remove();
                    notifyObservers();
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return result;
        }

        void processChanges(EventCollection<IEvent<IClusterConfig>> collection) {
            List<String> newPlugins = loadPlugins();
            Iterator<String> iter = plugins.iterator();
            while (iter.hasNext()) {
                String plugin = iter.next();
                if (!newPlugins.contains(plugin)) {
                    iter.remove();
                    collection.add(new ClusterConfigEvent(JcrClusterConfig.this, new JavaPluginConfig(plugin),
                            ClusterConfigEvent.EventType.PLUGIN_REMOVED));
                }
            }
            for (String newPlugin : newPlugins) {
                if (!plugins.contains(newPlugin)) {
                    plugins.add(newPlugins.indexOf(newPlugin), newPlugin);
                    IPluginConfig config = get(newPlugins.indexOf(newPlugin));
                    collection.add(new ClusterConfigEvent(JcrClusterConfig.this, new JavaPluginConfig(config),
                            ClusterConfigEvent.EventType.PLUGIN_ADDED));
                }
            }
        }

        @SuppressWarnings("unchecked")
        void notifyObservers() {
            EventCollection<IEvent<IClusterConfig>> coll = new EventCollection<IEvent<IClusterConfig>>();
            processChanges(coll);
            if (coll.size() > 0) {
                IObservationContext<IClusterConfig> obContext = (IObservationContext<IClusterConfig>) getObservationContext();
                if (obContext != null) {
                    obContext.notifyObservers(coll);
                }
            }
        }

        void process(Event event, EventCollection<IEvent<IClusterConfig>> collection) {
            processChanges(collection);

            if (event.getType() != 0) {
                try {
                    String path = event.getPath();
                    path = path.substring(0, path.lastIndexOf('/'));

                    JcrItemModel model = new JcrItemModel(path, false);
                    Node root = getNode();
                    while (model != null && !root.isSame((Node) model.getObject())) {
                        if (model.exists()) {
                            Node node = (Node) model.getObject();
                            if (node.isNodeType(FrontendNodeType.NT_PLUGIN)) {
                                IPluginConfig config = wrapConfig(node);
                                collection.add(new ClusterConfigEvent(JcrClusterConfig.this, config,
                                        ClusterConfigEvent.EventType.PLUGIN_CHANGED));
                                break;
                            }
                        }
                        model = model.getParentModel();
                    }
                } catch (RepositoryException ex) {
                    log.error("unable to find plugin configuration for event", ex);
                }
            } else {
                collection.add(new ClusterConfigEvent(JcrClusterConfig.this, null,
                        ClusterConfigEvent.EventType.PLUGIN_CHANGED));
            }
        }

        private boolean isPropertyEvent(final Event event) {
            return event.getType() == Event.PROPERTY_ADDED
                    || event.getType() == Event.PROPERTY_CHANGED
                    || event.getType() == Event.PROPERTY_REMOVED;
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
    private JcrEventListener listener;

    public JcrClusterConfig(JcrNodeModel nodeModel) {
        super(nodeModel);
        configs = new PluginList();
    }

    public List<IPluginConfig> getPlugins() {
        return Collections.unmodifiableList(configs);
    }

    public void setPlugins(List<IPluginConfig> plugins) {
        Set<String> newNames = new TreeSet<String>();
        for (IPluginConfig config : plugins) {
            newNames.add(config.getName());
        }

        // clean up
        Set<String> oldNames = new TreeSet<String>();
        Iterator<IPluginConfig> iter = configs.iterator();
        while (iter.hasNext()) {
            IPluginConfig config = iter.next();
            if (!newNames.contains(config.getName())) {
                iter.remove();
            } else {        
                oldNames.add(config.getName());
            }
        }

        // add
        for (IPluginConfig config : plugins) {
            if (!oldNames.contains(config.getName())) {
                configs.add(config);
            }
        }

        // reorder
        if (plugins.size() >= 2) {
            try {
                Node node = getNodeModel().getNode();
                for (int i = plugins.size() - 2; i >= 0; i--) {
                    node.orderBefore(plugins.get(i).getName(), plugins.get(i + 1).getName());
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public List<String> getServices() {
        return getList(FrontendNodeType.FRONTEND_SERVICES);
    }

    public List<String> getReferences() {
        return getList(FrontendNodeType.FRONTEND_REFERENCES);
    }

    public List<String> getProperties() {
        return getList(FrontendNodeType.FRONTEND_PROPERTIES);
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
        return new JcrValueList<String>(new JcrPropertyModel<String>(getNodeModel().getItemModel().getPath() + "/"
                + key), PropertyType.STRING);
    }

    String getPluginName(Node node) throws RepositoryException {
        return wrapConfig(node).getName();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JcrClusterConfig) {
            return super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 3499 ^ super.hashCode();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void startObservation() {
        IObservationContext obContext = getObservationContext();
        String path = getNodeModel().getItemModel().getPath();
        int events = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                | Event.PROPERTY_REMOVED;
        listener = new JcrEventListener(obContext, events, path, true, null, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(EventIterator events) {
                IObservationContext<IClusterConfig> obContext = (IObservationContext<IClusterConfig>) getObservationContext();
                EventCollection<IEvent<IClusterConfig>> coll = new EventCollection<IEvent<IClusterConfig>>();
                while (events.hasNext()) {
                    configs.process(events.nextEvent(), coll);
                }
                if (coll.size() > 0) {
                    obContext.notifyObservers(coll);
                }
            }
        };

        listener.start();
    }

    @Override
    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

}
