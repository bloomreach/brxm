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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public abstract class AbstractDestinationDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected JcrNodeModel destination;
    protected String name;

    private IModel title;
    private IRenderService dialogRenderer;
    private IClusterControl control;

    public AbstractDestinationDialog(AbstractWorkflowPlugin workflowPlugin, IModel title, IModel question) {
        super(workflowPlugin);
        this.title = title;
        this.destination = null;
        add(new Label("question", question));
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));

        IPluginContext context = workflowPlugin.getPluginContext();
        IPluginConfig config = workflowPlugin.getPluginConfig();
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
        IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
        control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
        IClusterConfig decorated = control.getClusterConfig();
        String modelServiceId = decorated.getString("wicket.model.folder");
        ModelReference modelService;
        modelService = new ModelReference<IModel>(modelServiceId, getModel()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void setModel(IModel model) {
                AbstractDestinationDialog.this.destination = null;
                if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel)model).getNode() != null) {
                    destination = (JcrNodeModel) model;
                }
                super.setModel(model);
            }
        };
        modelService.init(context);

        control.start();

        dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind(null, "picker");
        add(dialogRenderer.getComponent());
    }

    @Override
    public void render(PluginRequestTarget target) {
        if(dialogRenderer != null) {
            dialogRenderer.render(target);
        }
        super.render(target);
    }

    @Override
    public final void onClose() {
        super.onClose();
        dialogRenderer.unbind();
        dialogRenderer = null;
        control.stop();
    }
    
    public IModel getTitle() {
        return title;
    }
    
    @Override
    public void onDetach() {
        if(destination != null) {
            destination.detach();
        }
        super.onDetach();
    }
}
