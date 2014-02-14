/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceContext;

public final class ContextWorkflowManagerPlugin extends AbstractWorkflowManagerPlugin {

    private static final long serialVersionUID = 1L;

    private final IPluginContext pluginContext;

    public ContextWorkflowManagerPlugin(IPluginContext context, IPluginConfig config) {
        super(new ServiceContext(context), config);
        this.pluginContext = context;

        Component menu;
        replace(menu = new Label("menu"));
        Component v;
        replace(v = new Label("view"));
        v.setVisible(false);
        menu.setVisible(false);
        v.setOutputMarkupId(true);
        menu.setOutputMarkupId(true);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final ServiceContext context = (ServiceContext) getPluginContext();
        context.attachTo(pluginContext);
    }

    @Override
    protected void onRemove() {
        super.onRemove();

        updateMenu(Collections.<Node>emptySet());

        final ServiceContext context = (ServiceContext) getPluginContext();
        context.stop();

        setFlag(FLAG_INITIALIZED, false);
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

        updateMenu(nodeSet);
    }

    private void updateMenu(final Set<Node> nodeSet) {
        MenuHierarchy menu = buildMenu(nodeSet);
        menu.flatten();
        MenuDrop dropdown = new MenuDrop("menu", null, menu);
        replace(dropdown);
        dropdown.setVisible(true);
    }

    @Override
    protected void onBeforeRender() {
        populate();
        super.onBeforeRender();
    }
}
