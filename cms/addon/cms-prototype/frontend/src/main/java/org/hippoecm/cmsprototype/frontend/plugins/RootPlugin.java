/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.perspectives.BrowserPerspective;
import org.hippoecm.cmsprototype.frontend.plugins.perspectives.EditPerspective;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;


public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private List tabs;

    public RootPlugin(String id, final JcrNodeModel model) {
        super(id, model);

        tabs = new ArrayList();
        add(new AjaxTabbedPanel("tabs", tabs));
    }

    @Override
    public void addChildren(PluginConfig pluginConfig) {
        List children = pluginConfig.getChildren(new PluginDescriptor(this));
        Iterator it = children.iterator();
        while (it.hasNext()) {
            final PluginDescriptor childDescriptor = (PluginDescriptor) it.next();

            tabs.add(new AbstractTab(new Model(childDescriptor.getId())) {
                private static final long serialVersionUID = 1L;

                public Panel getPanel(String panelId) {
                    PluginDescriptor tabDescriptor = new PluginDescriptor(childDescriptor.getPath(), childDescriptor
                            .getClassName(), panelId);
                    return new PluginFactory(tabDescriptor).getPlugin((JcrNodeModel) getModel());
                }
            });
        }
    }

    public void update(final AjaxRequestTarget target, JcrEvent jcrEvent) {
    }
    
}
