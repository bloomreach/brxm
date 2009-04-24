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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.upload.UploadDialog;
import org.hippoecm.frontend.plugins.standardworkflow.FolderWorkflowPlugin;

public class GalleryWorkflowPlugin extends FolderWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public GalleryWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("add", new StringResourceModel("add-image", this, null, "Add")) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "image-add-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    UploadDialog dialog = new UploadDialog(GalleryWorkflowPlugin.this.getPluginContext(),
                                                           GalleryWorkflowPlugin.this.getPluginConfig(),
                                                           new JcrNodeModel(((WorkflowDescriptorModel)GalleryWorkflowPlugin.this.getModel()).getNode()));
                    return dialog;
                } catch (RepositoryException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                    return null;
                }
            }
        });
    }
}
