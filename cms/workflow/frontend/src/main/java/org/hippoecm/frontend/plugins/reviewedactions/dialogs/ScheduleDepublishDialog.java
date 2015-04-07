/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.dialog.ReferringDocumentsView;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditorManager;

public class ScheduleDepublishDialog extends AbstractWorkflowDialogRestyling<Node> {

    public ScheduleDepublishDialog(IWorkflowInvoker action, JcrNodeModel nodeModel, IModel<Date> dateModel, IEditorManager editorMgr) {
        super(nodeModel, action);

        ReferringDocumentsProvider provider = new ReferringDocumentsProvider(nodeModel, false);
        add(new ReferringDocumentsView("refs", provider, editorMgr));

        addOrReplace(new DatePickerComponent(Dialog.BOTTOM_LEFT_ID, dateModel, new ResourceModel("schedule-depublish-text")));

        setFocusOnCancel();

        add(CssClass.append("hippo-window"));
        add(CssClass.append("schedule-depublication-dialog"));
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of(getString("schedule-depublish-title"));
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE_AUTO;
    }
}
