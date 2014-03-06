/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.richtext.dialog;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerController;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;

public abstract class AbstractBrowserDialog<T extends RichTextEditorDocumentLink> extends AbstractRichTextEditorDialog<T> {
    private static final long serialVersionUID = 1L;

    private final IPluginContext context;
    private final IPluginConfig config;

    private final NodePickerController controller;

    public AbstractBrowserDialog(IPluginContext context, IPluginConfig config, IModel<T> model) {
        super(model);

        this.context = context;
        this.config = config;

        controller = new NodePickerController(context, NodePickerControllerSettings.fromPluginConfig(config)) {

            @Override
            protected IModel<Node> getInitialModel() {
                return (IModel<Node>) getModelObject().getLinkTarget();
            }

            @Override
            protected IModel<Node> getPropertyNodeModel() {
                // Not yet implemented
                return null;
            }

            @Override
            protected void onSelect(boolean isValid) {
                IModel<Node> selectedModel = getSelectedModel();
                if(isValid && selectedModel != null) {
                    getModelObject().setLinkTarget(selectedModel);
                    onModelSelected(selectedModel);
                    checkState();
                } else {
                    setOkEnabled(false);
                }
            }
        };

        add(controller.create("content"));
    }

    protected void onModelSelected(IModel<Node> model) {

    }

    protected IModel<Node> getFolderModel() {
        return controller.getFolderModel();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        if(controller.getRenderer() != null) {
            controller.getRenderer().render(target);
        }
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=855,height=485");
    }

    @Override
    public void onClose() {
        controller.onClose();
        super.onClose();
    }

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

}
