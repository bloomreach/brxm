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
package org.hippoecm.frontend.i18n.model;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
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
public class NodeTranslator extends NodeModelWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(NodeTranslator.class);

    private static final long serialVersionUID = 1L;

    private NodeNameModel name;
    private transient TreeMap<String, Property> properties;

    public NodeTranslator(JcrNodeModel nodeModel) {
        super(nodeModel);
        name = new NodeNameModel();
    }

    public IModel getNodeName() {
        return name;
    }

    public IModel getPropertyName(String property) {
        attach();
        if (!properties.containsKey(property)) {
            properties.put(property, new Property(property));
        }
        return properties.get(property).name;
    }

    public IModel getValueName(String property, String value) {
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

    private class NodeNameModel extends LoadableDetachableModel implements IObservable {
        private static final long serialVersionUID = 1L;

        private IObservationContext obContext;
        private IObserver observer;

        @Override
        protected Object load() {
            Node node = nodeModel.getNode();
            String name = "node name";
            if (node != null) {
                try {
                    // return the name specified by the hippo:translated mixin,
                    // falling back to the decoded node name itself.
                    name = NodeNameCodec.decode(node.getName());
                    if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                        Locale locale = Session.get().getLocale();
                        NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            if (child.isNodeType(HippoNodeType.NT_TRANSLATION)
                                    && !child.hasProperty(HippoNodeType.HIPPO_PROPERTY)) {
                                String language = child.getProperty("hippo:language").getString();
                                if (locale.getLanguage().equals(language)) {
                                    return child.getProperty("hippo:message").getString();
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
        }

        void innerDetach() {
            super.onDetach();
        }

        @Override
        public void onDetach() {
            NodeTranslator.this.detach();
        }

        public void setObservationContext(IObservationContext context) {
            this.obContext = context;
        }

        public void startObservation() {
            final JcrNodeModel parentModel = nodeModel.getParentModel();
            obContext.registerObserver(observer = new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return parentModel;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    obContext.notifyObservers(new EventCollection<IEvent>(events));
                }

            });
        }

        public void stopObservation() {
            obContext.unregisterObserver(observer);
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

        IModel getValue(String name) {
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

        class PropertyNameModel extends LoadableDetachableModel {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                Property.this.attach();
                String name = property;
                Node node = nodeModel.getNode();
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

        class PropertyValueModel extends LoadableDetachableModel {
            private static final long serialVersionUID = 1L;

            private String value;

            PropertyValueModel(String value) {
                this.value = value;
            }

            @Override
            protected Object load() {
                Property.this.attach();
                String name = property;
                Node node = nodeModel.getNode();
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
