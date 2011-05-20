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
package org.hippoecm.frontend.plugins.console.menu.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutocompleteTextField;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDialog extends AbstractDialog<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(NodeDialog.class);
    
    private String name;
    private String type = "nt:unstructured";

    private final Map<String,String> choices = new HashMap<String, String>();
    private final IModelReference modelReference;

    public NodeDialog(IModelReference modelReference) {
        this.modelReference = modelReference;
        JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
        setModel(nodeModel);
        
        // list defined child node names and types for automatic completion
        Node node = nodeModel.getNode();
        try {
            NodeType pnt = node.getPrimaryNodeType();
            for (NodeDefinition nd : pnt.getChildNodeDefinitions()) {
            	if (!nd.getName().equals("*")) {
                	choices.put(nd.getName(), nd.getDefaultPrimaryTypeName());
            	}
            }
            for (NodeType nt : node.getMixinNodeTypes()) {
            	for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
            		if (!nd.getName().equals("*")) {
                		choices.put(nd.getName(), nd.getDefaultPrimaryTypeName());
            		}
            	}
            }
        }
        catch (RepositoryException e) {
        	log.warn("Unable to populate autocomplete list for child node names", e);
        }
        
        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false);
        settings.setUseSmartPositioning(true);
        settings.setShowCompleteListOnFocusGain(true);
        
        final Model<String> typeModel = new Model<String>() {
            private static final long serialVersionUID = 1L;
            @Override public String getObject() {
                if (name != null && choices.containsKey(name)) {
                    type = choices.get(name);
                }
                return type;
            }
            @Override public void setObject(String s) {
                type = s;
            }
        };
        final AutoCompleteTextField<String> typeField = new AutoCompleteTextField<String>("type", typeModel, settings) {
            private static final long serialVersionUID = 1L;
            @Override protected Iterator<String> getChoices(String input) {
                if (Strings.isEmpty(input)) {
                    return Collections.EMPTY_LIST.iterator();
                }
                List<String> result = new ArrayList<String>();
                for (String nodeType : choices.values()) {
                    if (nodeType.startsWith(input)) {
                        result.add(nodeType);
                    }
                }
                return result.iterator();
            }
        };
        typeField.add(CSSPackageResource.getHeaderContribution(DefaultCssAutocompleteTextField.class, "DefaultCssAutocompleteTextField.css"));
        add(typeField);
        
        final Model<String> nameModel = new Model<String>() {
            private static final long serialVersionUID = 1L;
            @Override public String getObject() {
                return name;
            }
            @Override public void setObject(String s) {
                name = s;
            }
        };
        final AutoCompleteTextField<String> nameField = new AutoCompleteTextField<String>("name", nameModel, settings) {
			private static final long serialVersionUID = 1L;
			@Override protected Iterator<String> getChoices(String input) {
				if (Strings.isEmpty(input)) {
					return Collections.EMPTY_LIST.iterator();
				}
				List<String> result = new ArrayList<String>();
				for (String nodeName : choices.keySet()) {
					if (nodeName.startsWith(input)) {
						result.add(nodeName);
					}
				}
				return result.iterator();
			}
        };
        nameField.add(CSSPackageResource.getHeaderContribution(DefaultCssAutocompleteTextField.class, "DefaultCssAutocompleteTextField.css"));
        nameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(typeField);
            }
        });
        add(setFocus(nameField));
        
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) getModel();
            Node node = nodeModel.getNode().addNode(getName(), getType());

            modelReference.setModel(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            error(ex.toString());
        }
    }

    public IModel<String> getTitle() {
        return new Model<String>("Add a new Node");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    
    @Override
    public IValueMap getProperties() {
        return SMALL;
    }
}
