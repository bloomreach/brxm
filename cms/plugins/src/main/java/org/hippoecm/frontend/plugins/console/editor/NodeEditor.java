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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.data.EmptyDataProvider;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeEditor extends Form {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeEditor.class);

    @SuppressWarnings("unused")
    private String primaryType;
    private String mixinTypes;
    private PropertyProvider propertyProvider;
    private PropertiesEditor propertiesEditor;
    private NodeTypesEditor typesEditor;

    NodeEditor(String id) {
        super(id);
        setOutputMarkupId(true);

        add(new Label("primarytype", new PropertyModel(this, "primaryType")));
        add(new Label("types", new PropertyModel(this, "mixinTypes")));

        propertyProvider = new PropertyProvider(new EmptyDataProvider());
        propertiesEditor = new PropertiesEditor("properties", propertyProvider);
        add(propertiesEditor);

        typesEditor = new NodeTypesEditor("mixintypes", new ArrayList<String>(), null);
        add(typesEditor);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        IModel model = getModel();
        if ((model != null) && (model instanceof JcrNodeModel) && (((JcrNodeModel) model).getNode() != null)) {
            try {
                JcrNodeModel newModel = (JcrNodeModel) model;

                propertyProvider.setWrappedProvider(new PropertiesFilter(new JcrPropertiesProvider(newModel)));

                List<String> result = new ArrayList<String>();
                NodeType[] nodeTypes = newModel.getNode().getMixinNodeTypes();
                for (NodeType nodeType : nodeTypes) {
                    result.add(nodeType.getName());
                }
                typesEditor.setModelObject(result);
                typesEditor.setNodeModel(newModel);
                typesEditor.setVisible(true);
                propertiesEditor.setVisible(true);

                primaryType = newModel.getNode().getPrimaryNodeType().getName();
                mixinTypes = new String();
                for (NodeType type : nodeTypes) {
                    mixinTypes += type.getName() + ", ";
                }
                mixinTypes = StringUtils.substringBeforeLast(mixinTypes, ",");

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            typesEditor.setVisible(false);
            propertiesEditor.setVisible(false);
        }
    }

    private static class PropertyProvider implements IDataProvider {
        private static final long serialVersionUID = 1L;

        private IDataProvider wrapped;

        public PropertyProvider(IDataProvider wrapped) {
            this.wrapped = wrapped;
        }

        void setWrappedProvider(IDataProvider provider) {
            this.wrapped = provider;
        }

        public void detach() {
            wrapped.detach();
        }

        public Iterator iterator(int first, int count) {
            return wrapped.iterator(first, count);
        }

        public IModel model(Object object) {
            return wrapped.model(object);
        }

        public int size() {
            return wrapped.size();
        }
    }

    private class PropertiesFilter implements IDataProvider {
        private static final long serialVersionUID = 1L;

        private JcrPropertiesProvider decorated;
        private List<JcrPropertyModel> entries;

        public PropertiesFilter(JcrPropertiesProvider propertiesProvider) {
            decorated = propertiesProvider;
        }

        public Iterator<JcrPropertyModel> iterator(int first, int count) {
            load();
            return entries.subList(first, first + count).iterator();
        }

        public JcrPropertyModel model(Object object) {
            return (JcrPropertyModel) object;
        }

        public int size() {
            load();
            return entries.size();
        }

        public void detach() {
            entries = null;
            decorated.detach();
        }

        private void load() {
            entries = new ArrayList<JcrPropertyModel>();
            try {
                Iterator<Property> it = decorated.iterator(0, decorated.size());
                while (it.hasNext()) {
                    Property p = it.next();
                    if (!p.getName().equals("jcr:primaryType") && !p.getName().equals("jcr:mixinTypes")) {
                        entries.add(new JcrPropertyModel(p));
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            Collections.sort(entries, new PropertyComparator());
        }
    }

    private class PropertyComparator implements Comparator<JcrPropertyModel> {
        private static final long serialVersionUID = 1L;

        public int compare(JcrPropertyModel o1, JcrPropertyModel o2) {
            try {
                Property p1 = o1.getProperty();
                Property p2 = o2.getProperty();
                if (p1 == null) {
                    if (p2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (p2 == null) {
                    return -1;
                }

                Boolean p1_isProtected = p1.getDefinition().isProtected();
                Boolean p2_isProtected = p2.getDefinition().isProtected();
                int compare_protected = p1_isProtected.compareTo(p2_isProtected);
                if (compare_protected != 0) {
                    return compare_protected;
                }

                Boolean p1_isReference = ReferenceEditor.isReference(o1);
                Boolean p2_isReference = ReferenceEditor.isReference(o2);
                return p1_isReference.compareTo(p2_isReference);
            } catch (RepositoryException e) {
                return 0;
            }
        }
    }

}
