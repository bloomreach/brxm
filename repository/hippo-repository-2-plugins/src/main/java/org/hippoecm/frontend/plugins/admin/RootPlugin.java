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
package org.hippoecm.frontend.plugins.admin;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;

public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public RootPlugin(String id, JcrNodeModel model) {
        super(id, model);
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
        visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;
                try {
                    String newPluginClassname = model.getNode().getProperty("hippo:renderer").getString();
                    if (newPluginClassname != null) {
                        if (plugin.getId().equals("workflowPlugin")) {
                            PluginDescriptor pluginDescriptor = new PluginDescriptor(plugin.getPath(), newPluginClassname);
                            Plugin newPlugin = new PluginFactory(pluginDescriptor).getPlugin(model);
                            newPlugin.setRenderBodyOnly(true);

                            RootPlugin.this.replace(newPlugin);
                            target.addComponent(RootPlugin.this);
                        }
                    }
                } catch (RepositoryException e) {
                    //not a rendering node
                }

                if (!plugin.getRenderBodyOnly()) {
                    plugin.update(target, model);
                }

                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

}
