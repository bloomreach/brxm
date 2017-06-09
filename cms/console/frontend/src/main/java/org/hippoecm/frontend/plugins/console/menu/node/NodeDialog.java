/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.widgets.AutoCompleteTextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDialog extends AbstractDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(NodeDialog.class);

    private String name;
    private String type;

    private final Map<String, Collection<String>> namesToTypes = new HashMap<>();
    private final Map<String, Collection<String>> typesToNames = new HashMap<>();

    private final IModelReference<Node> modelReference;

    private final TextField<String> typeField;
    private final TextField<String> nameField;

    public NodeDialog(IModelReference<Node> modelReference) {
        this.modelReference = modelReference;
        final IModel<Node> nodeModel = modelReference.getModel();
        setModel(nodeModel);

        getParent().add(CssClass.append("node-dialog"));

        // list defined child node names and types for automatic completion
        final Node node = nodeModel.getObject();
        try {
            NodeType pnt = node.getPrimaryNodeType();
            for (NodeDefinition nd : pnt.getChildNodeDefinitions()) {
                if (!nd.isProtected()) {
                    for (NodeType nt : nd.getRequiredPrimaryTypes()) {
                        if (!nt.isAbstract()) {
                            addNodeType(nd, nt);
                        }
                        for (NodeType subnt : getDescendantNodeTypes(nt)) {
                            addNodeType(nd, subnt);
                        }
                    }
                }
            }
            for (NodeType nt : node.getMixinNodeTypes()) {
                for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
                    if (!nd.isProtected()) {
                        for (NodeType cnt : nd.getRequiredPrimaryTypes()) {
                            if (!cnt.isAbstract()) {
                                addNodeType(nd, cnt);
                            }
                            for (NodeType subnt : getDescendantNodeTypes(cnt)) {
                                addNodeType(nd, subnt);
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to populate autocomplete list for child node names", e);
        }

        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false);
        settings.setUseSmartPositioning(true);
        settings.setShowCompleteListOnFocusGain(true);
        settings.setShowListOnEmptyInput(true);
        // Setting a max height will trigger a correct recalculation of the height when the list of items is filtered
        settings.setMaxHeightInPx(400);

        final Model<String> typeModel = new Model<String>() {
            @Override
            public String getObject() {
                if (name != null && namesToTypes.containsKey(name)) {
                    Collection<String> types = namesToTypes.get(name);
                    if (types.size() == 1) {
                        type = types.iterator().next();
                    }
                }
                else if (namesToTypes.size() == 1) {
                    Collection<String> types = namesToTypes.values().iterator().next();
                    if (types.size() == 1) {
                        type = types.iterator().next();
                    }
                }
                return type;
            }

            @Override
            public void setObject(String s) {
                type = s;
            }
        };
        typeField = new AutoCompleteTextFieldWidget<String>("type", typeModel, settings) {
            @Override
            protected Iterator<String> getChoices(final String input) {
                Collection<String> result = new TreeSet<>();
                if (!Strings.isEmpty(name)) {
                    if (namesToTypes.get(name) != null) {
                        result.addAll(namesToTypes.get(name));
                    }
                    if (namesToTypes.get("*") != null) {
                        result.addAll(namesToTypes.get("*"));
                    }
                }
                else {
                    namesToTypes.values().forEach(result::addAll);
                }
                Iterator<String> resultIter = result.iterator();
                while (resultIter.hasNext()) {
                    if (!resultIter.next().toLowerCase().contains(input.toLowerCase())) {
                        resultIter.remove();
                    }
                }
                return result.iterator();
            }

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (isVisibleInHierarchy()) {
                    target.add(nameField);
                }
            }
        };
        typeField.setRequired(true);
        add(typeField);

        final Model<String> nameModel = new Model<String>() {
            @Override
            public String getObject() {
                if (type != null && typesToNames.containsKey(type)) {
                    if (name == null) {
                        Collection<String> names = typesToNames.get(type);
                        if (names.size() == 1) {
                            String _name = names.iterator().next();
                            if (!_name.equals("*")) {
                                name = _name;
                            }
                        }
                    }
                }
                else if (typesToNames.size() == 1) {
                    Collection<String> names = typesToNames.values().iterator().next();
                    if (names.size() == 1) {
                        String _name = names.iterator().next();
                        if (!_name.equals("*")) {
                            name = _name;
                        }
                    }
                }
                return name;
            }

            @Override
            public void setObject(String s) {
                name = s;
            }
        };
        nameField = new AutoCompleteTextFieldWidget<String>("name", nameModel, settings) {
            @Override
            protected Iterator<String> getChoices(String input) {
                Collection<String> result = new TreeSet<>();
                if (type != null && !type.isEmpty()) {
                    if (typesToNames.get(type) != null) {
                        result.addAll(typesToNames.get(type));
                    }
                }
                else {
                    for (String nodeName : namesToTypes.keySet()) {
                        if (!nodeName.equals("*") && nodeName.toLowerCase().contains(input.toLowerCase())) {
                            result.add(nodeName);
                        }
                    }
                }
                return result.iterator();
            }

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (isVisibleInHierarchy()) {
                    target.add(typeField);
                }
            }
        };
        nameField.setRequired(true);

        add(setFocus(nameField));
    }

    @Override
    public void onOk() {
        try {
            final IModel<Node> nodeModel = getModel();
            final Node node = nodeModel.getObject().addNode(name, type);

            modelReference.setModel(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            error(ex.toString());
        }
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("Add a new Node");
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

    private Collection<NodeType> getDescendantNodeTypes(NodeType nt) {
        Collection<NodeType> result = new HashSet<>();
        NodeTypeIterator subNodeTypes = nt.getDeclaredSubtypes();
        while (subNodeTypes.hasNext()) {
            NodeType subNodeType = subNodeTypes.nextNodeType();
            if (!subNodeType.isAbstract()) {
                result.add(subNodeType);
            }
            result.addAll(getDescendantNodeTypes(subNodeType));
        }
        return result;
    }

    private void addNodeType(NodeDefinition nd, NodeType nt) {
        Collection<String> types = namesToTypes.get(nd.getName());
        if (types == null) {
            types = new HashSet<>(5);
            namesToTypes.put(nd.getName(), types);
        }
        types.add(nt.getName());
        Collection<String> names = typesToNames.get(nt.getName());
        if (names == null) {
            names = new HashSet<>(5);
            typesToNames.put(nt.getName(), names);
        }
        names.add(nd.getName());
    }
}
