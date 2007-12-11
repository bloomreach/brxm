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
package org.hippoecm.frontend.plugins.reviewedactions;

import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.depublish.DePublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.disposeeditableinstance.DisposeEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.obtaineditableinstance.ObtainEditableInstanceDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.publish.PublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdeletion.RequestDeletionDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdepublication.RequestDePublicationDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestpublication.RequestPublicationDialog;

public class FullReviewedActionsWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    public FullReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, final JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        
        add(new DialogLink("obtainEditableInstance-dialog", "Obtain editable copy", ObtainEditableInstanceDialog.class, model));
        add(new DialogLink("disposeEditableInstance-dialog", "Dispose editable copy", DisposeEditableInstanceDialog.class, model));
        add(new DialogLink("requestPublication-dialog", "Request publication", RequestPublicationDialog.class, model));
        add(new DialogLink("requestDePublication-dialog", "Request unpublication", RequestDePublicationDialog.class, model));
        add(new DialogLink("requestDeletion-dialog", "Request delete", RequestDeletionDialog.class, model));
        add(new DialogLink("publish-dialog", "Publish", PublishDialog.class, model));
        add(new DialogLink("dePublish-dialog", "Unpublish", DePublishDialog.class, model));
        add(new DialogLink("delete-dialog", "Unpublish and/or delete", DeleteDialog.class, model));
    }

}
