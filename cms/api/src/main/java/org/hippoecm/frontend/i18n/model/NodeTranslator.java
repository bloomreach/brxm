/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.i18n.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translator of node names, property names and values.  When a node has the mixin
 * hippo:translated set, it is used to lookup the translated strings.  When no such mixin
 * is present or the node doesn't exist, the last element of the node path is returned.
 */
public class NodeTranslator extends NodeModelWrapper<NodeTranslator> {

    final static Logger log = LoggerFactory.getLogger(NodeTranslator.class);

    private static final long serialVersionUID = 1L;

    private NodeNameModel name;
    private transient TreeMap<String, Property> properties;

    public NodeTranslator(IModel<Node> nodeModel) {
        super(nodeModel);
        name = new NodeNameModel();
    }

    public IModel<String> getNodeName() {
        return name;
    }

    public IModel<String> getPropertyName(String property) {
        attach();
        if (!properties.containsKey(property)) {
            properties.put(property, new Property(property));
        }
        return properties.get(property).name;
    }

    public IModel<String> getValueName(String property, String value) {
        attach();
        if (!properties.containsKey(property)) {
            properties.put(property, new Property(property));
        }
        return properties.get(property).getValue(value);
    }

    @Override
    public void detach() {
        super.detach();
        name.innerDetach();
        properties = null;
    }

    private void attach() {
        if (properties == null) {
            properties = new TreeMap<String, Property>();
        }
    }

    private class NodeNameModel extends LoadableDetachableModel<String> implements IObservable {
        private static final long serialVersionUID = 1L;

        private IObservationContext<NodeNameModel> obContext;
        private Set<IObserver<JcrNodeModel>> observers = new HashSet<IObserver<JcrNodeModel>>();
        private Set<JcrNodeModel> accessed;

        @Override
        protected String load() {
            if (obContext != null) {
                stopObservation();
            }

            accessed = new HashSet<JcrNodeModel>();

            final JcrNodeModel parentModel = nodeModel.getParentModel();
            if (parentModel != null) {
                accessed.add(parentModel);
            }

            try {
                Node node = nodeModel.getObject();
                String name = "node name";
                if (node != null) {
                    try {
                        // return the name specified by the hippo:translated mixin,
                        // falling back to the decoded node name itself.
                        name = NodeNameCodec.decode(node.getName());
                        accessed.add(new JcrNodeModel(node));
                        if (!node.isNodeType(HippoNodeType.NT_TRANSLATED) && node.isNodeType(HippoNodeType.NT_DOCUMENT)
                                && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)
                                && node.getParent().isNodeType(HippoNodeType.NT_TRANSLATED)) {
                            node = node.getParent();
                        }
                        if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                accessed.add(new JcrNodeModel(child));
                                if (child.isNodeType(HippoNodeType.NT_TRANSLATION)
                                        && !child.hasProperty(HippoNodeType.HIPPO_PROPERTY)) {
                                    String language = child.getProperty("hippo:language").getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty("hippo:message").getString();
                                    }
                                    if (language.equals("")) {
                                        name = child.getProperty("hippo:message").getString();
                                    }
                                }
                            }
                        }

                        // when the node is not translated, return the decoded name at
                        // the time of version creation.  Fall back to handle name if necessary.
                        if (node.isNodeType("nt:frozenNode") && (node.getParent() instanceof Version)) {
                            try {
                                String historyUuid = ((Version) node.getParent()).getContainingHistory().getUUID();
                                Version parentVersion = JcrHelper.getVersionParent((Version) node.getParent());
                                // locate child.  Only direct children are found.
                                NodeIterator children = parentVersion.getNode("jcr:frozenNode").getNodes();
                                while (children.hasNext()) {
                                    Node child = children.nextNode();
                                    if (child.isNodeType("nt:versionedChild")) {
                                        String ref = child.getProperty("jcr:childVersionHistory").getString();
                                        if (ref.equals(historyUuid)) {
                                            return NodeNameCodec.decode(child.getName());
                                        }
                                    }
                                }
                                Node parent = node.getSession().getNodeByUUID(
                                        parentVersion.getContainingHistory().getVersionableUUID());
                                if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    return NodeNameCodec.decode(parent.getName());
                                }
                            } catch (ItemNotFoundException ex) {
                                // ignore, use name of node itself
                            }

                            // unable to resolve parent
                            String uuid = node.getProperty("jcr:frozenUuid").getString();
                            try {
                                Node docNode = node.getSession().getNodeByUUID(uuid);
                                return NodeNameCodec.decode(docNode.getName());
                            } catch (ItemNotFoundException ex) {
                                // ignore
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    String path = nodeModel.getItemModel().getPath();
                    if (path != null) {
                        name = path.substring(path.lastIndexOf('/') + 1);
                        if (name.indexOf('[') > 0) {
                            name = name.substring(0, name.indexOf('['));
                        }
                        name = NodeNameCodec.decode(name);
                    }
                }
                return name;
            } finally {
                if (obContext != null) {
                    startObservation();
                }
            }
        }

        void innerDetach() {
            super.detach();
        }

        @Override
        public void detach() {
            if (accessed != null) {
                for (JcrNodeModel model : accessed) {
                    model.detach();
                }
            }
            NodeTranslator.this.detach();
        }

        @SuppressWarnings("unchecked")
        public void setObservationContext(IObservationContext<?> context) {
            this.obContext = (IObservationContext<NodeNameModel>) context;
        }

        public void startObservation() {
            reregisterObservers();
        }

        private void reregisterObservers() {
            if (!isAttached()) {
                getObject();
            }

            for (IObserver observer : observers) {
                obContext.unregisterObserver(observer);
            }
            observers.clear();

            for (final JcrNodeModel model : accessed) {
                IObserver observer = new IObserver<JcrNodeModel>() {
                    private static final long serialVersionUID = 1L;

                    public JcrNodeModel getObservable() {
                        return model;
                    }

                    public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
                        IEvent<NodeNameModel> event = new IEvent<NodeNameModel>() {

                            public NodeNameModel getSource() {
                                return NodeNameModel.this;
                            }

                        };
                        EventCollection<IEvent<NodeNameModel>> collection = new EventCollection<IEvent<NodeNameModel>>();
                        collection.add(event);
                        obContext.notifyObservers(collection);
                        reregisterObservers();
                    }

                };

                obContext.registerObserver(observer);
                observers.add(observer);
            }
        }

        public void stopObservation() {
            for (IObserver observer : observers) {
                obContext.unregisterObserver(observer);
            }
            observers.clear();
        }

    }

    private class Property implements IDetachable {
        private static final long serialVersionUID = 1L;

        String property;
        PropertyNameModel name;
        Map<String, PropertyValueModel> values;
        transient boolean attached = false;

        Property(String property) {
            this.property = property;
            this.name = new PropertyNameModel();
            attached = true;
        }

        IModel<String> getValue(String name) {
            if (values == null) {
                values = new TreeMap<String, PropertyValueModel>();
            }
            if (!values.containsKey(name)) {
                values.put(name, new PropertyValueModel(name));
            }
            return values.get(name);
        }

        void attach() {
            attached = true;
        }

        public void detach() {
            if (attached) {
                name.onDetachProperty();
                if (values != null) {
                    for (Map.Entry<String, PropertyValueModel> entry : values.entrySet()) {
                        entry.getValue().onDetachProperty();
                    }
                }
                nodeModel.detach();
                attached = false;
            }
        }

        class PropertyNameModel extends LoadableDetachableModel<String> {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                Property.this.attach();
                String name = property;
                Node node = nodeModel.getObject();
                if (node != null) {
                    try {
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && child.hasProperty("hippo:property")
                                        && !child.hasProperty("hippo:value")) {
                                    if (child.getProperty("hippo:property").getString().equals(property)) {
                                        String language = child.getProperty("hippo:language").getString();
                                        if (locale.getLanguage().equals(language)) {
                                            return child.getProperty("hippo:message").getString();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
                return name;
            }

            void onDetachProperty() {
                super.detach();
            }

            @Override
            public void detach() {
                Property.this.detach();
            }
        }

        class PropertyValueModel extends LoadableDetachableModel<String> {
            private static final long serialVersionUID = 1L;

            private String value;

            PropertyValueModel(String value) {
                this.value = value;
            }

            @Override
            protected String load() {
                Property.this.attach();
                String name = property;
                Node node = nodeModel.getObject();
                if (node != null) {
                    try {
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && child.hasProperty("hippo:property")
                                        && child.hasProperty("hippo:value")) {
                                    if (child.getProperty("hippo:property").getString().equals(property)
                                            && child.getProperty("hippo:value").getString().equals(value)) {
                                        String language = child.getProperty("hippo:language").getString();
                                        if (locale.getLanguage().equals(language)) {
                                            return child.getProperty("hippo:message").getString();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
                return name;
            }

            void onDetachProperty() {
                super.detach();
            }

            @Override
            public void detach() {
                Property.this.detach();
            }

        }

    }

}
