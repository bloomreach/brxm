/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.workflow;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FolderTranslationsWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    final static String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    private static Logger log = LoggerFactory.getLogger(FolderTranslationsWorkflowPlugin.class);

    public FolderTranslationsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("folder-translations", new StringResourceModel("folder-translations", this),
                context, (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TRANSLATE);
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel workflowModel = getModel();
                try {
                    JcrNodeModel nodeModel = new JcrNodeModel(workflowModel.getNode());
                    StringResourceModel title = new StringResourceModel("folder-translations-title", FolderTranslationsWorkflowPlugin.this);
                    return new FolderTranslationsDialog(this, title, nodeModel, getLocaleProvider());
                } catch (RepositoryException e) {
                    throw new RuntimeException("Could not retrieve node for workflow", e);
                }
            }
        });
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

}
