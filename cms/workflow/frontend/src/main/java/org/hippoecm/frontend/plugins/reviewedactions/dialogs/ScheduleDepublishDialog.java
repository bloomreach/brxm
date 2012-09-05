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

import java.util.Date;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.editor.workflow.dialog.ReferringDocumentsView;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IEditorManager;

public class ScheduleDepublishDialog extends WorkflowAction.DateDialog {

    private static final long serialVersionUID = 1L;

    public ScheduleDepublishDialog(WorkflowAction action, JcrNodeModel nodeModel, PropertyModel<Date> dateModel, IEditorManager editorMgr) {
        action.super(new ResourceModel("schedule-depublish-text"), dateModel);

        ReferringDocumentsProvider provider = new ReferringDocumentsProvider(nodeModel, false);
        add(new ReferringDocumentsView("refs", provider, editorMgr));
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("schedule-depublish-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return LARGE;
    }
}
