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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;

public class NodeDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private MenuPlugin plugin;
    private String name;
    private String type = "nt:unstructured";

    public NodeDialog(MenuPlugin plugin) {
        this.plugin = plugin;
        add(setFocus(new TextField("name", new PropertyModel(this, "name"))));
        add(new TextField("type", new PropertyModel(this, "type")));
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) plugin.getDefaultModel();
            Node node = nodeModel.getNode().addNode(getName(), getType());

            plugin.setDefaultModel(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model("Add a new Node");
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
