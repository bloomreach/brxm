/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.UnpublishedReferenceNodeProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.service.IEditorManager;

public class SchedulePublishDialog extends WorkflowAction.DateDialog {

    private static final long serialVersionUID = 1L;

    public SchedulePublishDialog(WorkflowAction action, JcrNodeModel nodeModel, PropertyModel dateModel,
            IEditorManager editorMgr) {
        action.super(new ResourceModel("schedule-publish-text"), dateModel);

        UnpublishedReferenceNodeProvider provider = new UnpublishedReferenceNodeProvider(
                new UnpublishedReferenceProvider(new ReferenceProvider(nodeModel)));
        add(new UnpublishedReferencesView("links", provider, editorMgr));
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("schedule-publish-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return LARGE;
    }
}
