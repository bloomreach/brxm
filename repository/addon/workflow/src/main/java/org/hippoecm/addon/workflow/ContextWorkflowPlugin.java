/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public final class ContextWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ContextWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        Component menu;
        add(menu = new Label("menu"));
        Component v;
        add(v = new Label("view"));
        v.setVisible(false);
        menu.setVisible(false);
        v.setOutputMarkupId(true);
        menu.setOutputMarkupId(true);
        setOutputMarkupId(true);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
    }

    private void populate() {
        Set<Node> nodeSet = new LinkedHashSet<Node>();
        if(getDefaultModel() instanceof JcrNodeModel) {
            Node node = ((JcrNodeModel)getDefaultModel()).getNode();
            if(node != null) {
                nodeSet.add(node);
            }
        }
        MenuHierarchy menu = buildMenu(nodeSet);
        menu.flatten();
        MenuDrop dropdown = new MenuDrop("menu", null, menu);
        addOrReplace(dropdown);
        dropdown.setVisible(true);
    }
    
    @Override
    protected void onBeforeRender() {
        populate();
        super.onBeforeRender();
    }
}
