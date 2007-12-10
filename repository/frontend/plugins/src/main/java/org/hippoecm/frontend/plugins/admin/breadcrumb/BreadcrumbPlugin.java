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
package org.hippoecm.frontend.plugins.admin.breadcrumb;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * A simple plugin displaying the complete JCR path of the current JcrNodeModel.
 */
public class BreadcrumbPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private String nodePath;

    public BreadcrumbPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        nodePath = model.getItemModel().getPath();
        add(new Label("path", new PropertyModel(this, "nodePath")));
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel newModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (newModel != null) {
            JcrNodeModel nodeModel = newModel;
            setNodeModel(nodeModel);
            nodePath = nodeModel.getItemModel().getPath();
        }
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }

}
