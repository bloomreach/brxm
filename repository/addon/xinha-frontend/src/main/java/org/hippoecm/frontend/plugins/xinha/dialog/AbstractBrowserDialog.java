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

package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.xinha.XinhaPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBrowserDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AbstractBrowserDialog.class);

    protected final IPluginContext context;
    protected final IPluginConfig config;
    private ModelService<IModel> modelService;
    private IClusterControl control;
    protected IRenderService dialogRenderer;

    public AbstractBrowserDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(model);

        this.context = context;
        this.config = config;

        final Button remove = new AjaxButton("button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onRemove();
                if (!hasError()) {
                    closeDialog();
                }
            }

            @Override
            public boolean isVisible() {
                return hasRemoveButton();
            }
        };
        remove.add(new Label("label", "Remove"));
        addButton(remove);

        add(createContentPanel("content"));
    }

    protected Component createContentPanel(String contentId) {
        //Get PluginConfigService
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        //Lookup clusterConfig from IPluginContext
        IClusterConfig cluster = pluginConfigService.getCluster(config.getString("cluster.name"));
        control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
        IClusterConfig decorated = control.getClusterConfig();

        //save modelServiceId and dialogServiceId in cluster config
        String modelServiceId = decorated.getString("wicket.model");
        IModel model = ((DocumentLink) getModelObject()).getNodeModel();
        modelService = new ModelService<IModel>(modelServiceId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void setModel(IModel model) {
                JcrNodeModel newModel = findNewModel(model);
                if (newModel != null) {
                    DocumentLink link = (DocumentLink) getModelObject();
                    JcrNodeModel currentModel = link.getNodeModel();
                    if (!newModel.equals(currentModel)) {
                        link.setNodeModel(newModel);
                        checkState();
                    }
                }

                super.setModel(newModel);
            }
        };
        modelService.init(context);

        control.start();

        dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind((IRenderService) getComponent().findParent(XinhaPlugin.class), contentId);
        return dialogRenderer.getComponent();
    }

    public final void onClose() {
        dialogRenderer.unbind();
        dialogRenderer = null;
        control.stop();
        modelService.destroy();

        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            String script;
            if (cancelled) {
                script = getCancelScript();
            } else {
                script = getCloseScript();
            }
            target.getHeaderResponse().renderOnDomReadyJavascript(script);
        }
        onCloseDialog();
    }

    public void render(PluginRequestTarget target) {
        super.render(target);
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
    }

    public IModel getTitle() {
        return new StringResourceModel("dialog-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=850,height=455");
    }

    protected abstract JcrNodeModel findNewModel(IModel model);

    protected void checkState() {
        IPersistedMap link = (IPersistedMap) getModelObject();
        enableOk(link.isValid() && link.hasChanged());
    }

    protected boolean hasRemoveButton() {
        return ((IPersistedMap) getModelObject()).isExisting();
    }

    protected void onRemove() {
    }

    protected String getCancelScript() {
        return "if(openModalDialog != null){ openModalDialog.cancel(); }";
    }

    protected String getCloseScript() {
        String returnValue = ((IPersistedMap) getModelObject()).toJsString();
        return "if(openModalDialog != null){ openModalDialog.close(" + returnValue + "); }";
    }

    protected void onCloseDialog() {
    }

    protected void enableOk(boolean state) {
        ok.setEnabled(state);
    }

}
