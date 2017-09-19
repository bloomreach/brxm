/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerController;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.widgets.breadcrumb.NodeBreadcrumbWidget;

public abstract class AbstractBrowserDialog<T extends RichTextEditorDocumentLink> extends AbstractRichTextEditorDialog<T> {

    private final IPluginContext context;
    private final IPluginConfig config;

    private final NodePickerController controller;
    private final NodeBreadcrumbWidget breadcrumbs;

    public AbstractBrowserDialog(IPluginContext context, IPluginConfig config, IModel<T> model) {
        super(model);

        this.context = context;
        this.config = config;

        controller = new NodePickerController(context, NodePickerControllerSettings.fromPluginConfig(config)) {

            @Override
            protected IModel<Node> getInitialModel() {
                return getModelObject().getLinkTarget();
            }

            @Override
            protected void onSelect(boolean isValid) {
                IModel<Node> selectedModel = getSelectedModel();
                if (isValid && selectedModel != null) {
                    getModelObject().setLinkTarget(selectedModel);
                    onModelSelected(selectedModel);
                    checkState();
                } else {
                    setOkEnabled(false);
                }
            }

            @Override
            protected void onFolderSelected(final IModel<Node> model) {
                if (breadcrumbs != null) {
                    breadcrumbs.update(model);
                }
                super.onFolderSelected(model);
            }
        };

        add(controller.create("content"));

        addOrReplace(breadcrumbs = new NodeBreadcrumbWidget(Dialog.BOTTOM_LEFT_ID, null, controller.getRootPaths()) {
            @Override
            protected void onClick(final IModel<Node> model, final AjaxRequestTarget target) {
                controller.setSelectedFolder(model);
            }
        });
        breadcrumbs.update(controller.getFolderModel());
    }

    protected void initSelection() {
        controller.initSelection();
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(final String id) {
        return new FeedbackPanel(id, new ContainerFeedbackMessageFilter(this)) {{
            setOutputMarkupId(true);
        }};
    }

    protected void onModelSelected(IModel<Node> model) {
    }

    protected IModel<Node> getFolderModel() {
        return controller.getFolderModel();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        if (controller.getRenderer() != null) {
            controller.getRenderer().render(target);
        }
    }

    @Override
    protected void onDetach() {
        controller.detach();
        super.onDetach();
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
