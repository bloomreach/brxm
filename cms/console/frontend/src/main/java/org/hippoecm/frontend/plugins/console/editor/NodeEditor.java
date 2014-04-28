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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.EmptyDataProvider;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeEditor extends Form<Node> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeEditor.class);

    private static final NamespacePropertyComparator PROPERTY_COMPARATOR = new NamespacePropertyComparator();

    public static final String NONE_LABEL = "<none>";

    @SuppressWarnings("unused")
    private String name;
    @SuppressWarnings("unused")
    private String uuid;
    @SuppressWarnings("unused")
    private String nodePath;

    private NamespaceProvider namespaceProvider;
    private NamespacePropertiesEditor namespacePropertiesEditor;
    private NodeTypesEditor typesEditor;

    NodeEditor(String id, IModel<Node> model) {
        super(id, model);
        setOutputMarkupId(true);

        add(new ToggleHeader("toggle-header-0", "0", "General"));
        add(new Label("nodePath", new PropertyModel<String>(this, "nodePath")));
        add(new Label("name", new PropertyModel<String>(this, "name")));
        add(new Label("uuid", new PropertyModel<String>(this, "uuid")));

        add(new ToggleHeader("toggle-header-1", "1", "Types"));
        final TextFieldWidget primaryTypeWidget = new TextFieldWidget("primarytype", new PropertyModel<String>(this, "primaryType"));
        primaryTypeWidget.setSize("40");
        add(primaryTypeWidget);

        typesEditor = new NodeTypesEditor("mixintypes", model);
        add(typesEditor);
        add(new Label("types", new PropertyModel<String>(typesEditor, "mixinTypes")));

        add(new ToggleHeader("toggle-header-2", "2", "Properties"));
        namespaceProvider = new NamespaceProvider(new EmptyDataProvider());
        namespacePropertiesEditor = new NamespacePropertiesEditor("namespaces", namespaceProvider);
        add(namespacePropertiesEditor);

        add(new ToggleHeader("toggle-header-3", "3", "Mixin Types"));
        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        final IModel<Node> model = getModel();
        if (model != null && model.getObject() != null) {
            final Node node = model.getObject();
            try {
                namespaceProvider.setWrapped(new JcrPropertiesProvider(new JcrNodeModel(node)));

                nodePath = node.getPath();
                name = node.getName();
                uuid = node.getIdentifier();

                typesEditor.setModel(getModel());
                typesEditor.setVisible(true);

                namespacePropertiesEditor.setVisible(true);

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            typesEditor.setVisible(false);
            namespacePropertiesEditor.setVisible(false);
        }
    }

    @SuppressWarnings("unused")
    public String getPrimaryType() {
        final Node node = getModelObject();
        if (node != null) {
            try {
                return node.getPrimaryNodeType().getName();
            } catch (RepositoryException e) {
                log.error(e.getClass().getName() + ": " + e.getMessage(), e);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void setPrimaryType(String primaryType) {
        final Node node = getModelObject();
        if (node != null) {
            String oldPrimaryType = null;
            try {
                oldPrimaryType = node.getPrimaryNodeType().getName();
                node.setPrimaryType(primaryType);
            } catch (ConstraintViolationException e) {
                log.error("Cannot set primary type to {}", primaryType);
                if (oldPrimaryType != null) {
                    try {
                        node.setPrimaryType(oldPrimaryType);
                    } catch (RepositoryException e1) {
                        log.error("Failed to set primary node type to previous value");
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to set primary node type", e);
            }
        }
    }

    private static class NamespacePropertiesEditor extends DataView<NamespacePropertiesProvider> {
        @SuppressWarnings("unused")
        private static final long serialVersionUID = 1L;

        protected NamespacePropertiesEditor(String id, IDataProvider<NamespacePropertiesProvider> dataProvider) {
            super(id, dataProvider);
        }

        @Override
        protected void populateItem(final Item<NamespacePropertiesProvider> item) {
            NamespacePropertiesProvider propertiesProvider = item.getModelObject();

            final String namespace = propertiesProvider.getNamespace();

            final String namespaceHeading = namespace + " (" + propertiesProvider.size() + ")";
            item.add(new ToggleHeader("toggle-namespace", namespace, namespaceHeading));

            final WebMarkupContainer propertiesContainer = new WebMarkupContainer("propertiesContainer");
            propertiesContainer.setOutputMarkupId(true);
            propertiesContainer.setMarkupId("toggle-box-" + namespace);

            final PropertiesEditor propertiesEditor = new PropertiesEditor("properties", propertiesProvider);
            propertiesContainer.add(propertiesEditor);
            item.add(propertiesContainer);
        }
    }

    private static class NamespaceProvider implements IDataProvider<NamespacePropertiesProvider> {
        @SuppressWarnings("unused")
        private static final long serialVersionUID = 1L;

        private List<NamespacePropertiesProvider> namespaces;
        private IDataProvider<Property> wrapped;

        NamespaceProvider(IDataProvider<Property> wrapped) {
            this.wrapped = wrapped;
            namespaces = Collections.emptyList();
        }

        void setWrapped(IDataProvider<Property> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Iterator<? extends NamespacePropertiesProvider> iterator(final long first, final long count) {
            load();
            return namespaces.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            load();
            return namespaces.size();
        }

        @Override
        public IModel<NamespacePropertiesProvider> model(final NamespacePropertiesProvider object) {
            return new Model<NamespacePropertiesProvider>(object);
        }

        @Override
        public void detach() {
            namespaces = null;
            wrapped.detach();
        }

        private void load() {
            namespaces = new ArrayList<NamespacePropertiesProvider>();

            Map<String, NamespacePropertiesProvider> namespaceMap = new TreeMap<String, NamespacePropertiesProvider>();
            try {
                Iterator<? extends Property> it = wrapped.iterator(0, wrapped.size());
                while (it.hasNext()) {
                    Property p = it.next();
                    String propName = p.getName();
                    if (!propName.equals("jcr:primaryType") && !propName.equals("jcr:mixinTypes")) {
                        String propNamespace = new JcrName(propName).getNamespace();
                        if (propNamespace == null) {
                            propNamespace = NONE_LABEL;
                        }
                        NamespacePropertiesProvider propertiesProvider = namespaceMap.get(propNamespace);
                        if (propertiesProvider == null) {
                            propertiesProvider = new NamespacePropertiesProvider(propNamespace);
                            namespaceMap.put(propNamespace, propertiesProvider);
                        }

                        propertiesProvider.addProperty(p);
                    }
                }
                namespaces.addAll(namespaceMap.values());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    protected static class NamespacePropertiesProvider implements IDataProvider<Property> {
        @SuppressWarnings("unused")
        private static final long serialVersionUID = 1L;

        private final String namespace;
        private List<Property> properties;
        private boolean sorted;

        NamespacePropertiesProvider(String namespace) {
            this.namespace = namespace;
            properties = new ArrayList<Property>();
            sorted = true;
        }

        public String getNamespace() {
            return namespace;
        }

        void addProperty(Property p) {
            properties.add(p);
            if (properties.size() > 1) {
                sorted = false;
            }
        }
        
        @Override
        public Iterator iterator(final long first, final long count) {
            if (!sorted) {
                Collections.sort(properties, PROPERTY_COMPARATOR);
                sorted = true;
            }
            return properties.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            return properties.size();
        }

        @Override
        public IModel<Property> model(final Property object) {
            return new JcrPropertyModel(object);
        }

        @Override
        public void detach() {
            properties = null;
        }
    }

    private static class NamespacePropertyComparator implements Comparator<Property> {
        @SuppressWarnings("unused")
        private static final long serialVersionUID = 1L;

        public int compare(Property p1, Property p2) {
            try {
                if (p1 == null) {
                    if (p2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (p2 == null) {
                    return -1;
                }
                return p1.getName().compareTo(p2.getName());
            } catch (RepositoryException e) {
                throw new IllegalStateException("Error while comparing properties", e);
            }
        }
    }

}
