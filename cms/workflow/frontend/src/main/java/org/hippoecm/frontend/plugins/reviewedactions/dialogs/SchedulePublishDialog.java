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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.plugins.reviewedactions.UnpublishedReferenceNodeProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.service.IEditorManager;

public class SchedulePublishDialog extends WorkflowDialog<Node> {

    private final IModel<String> titleModel;

    public SchedulePublishDialog(final IWorkflowInvoker invoker, final IModel<Node> nodeModel,
                                 final IModel<Date> dateModel, final IEditorManager editorMgr) {
        super(invoker, nodeModel);

        final IModel<String> displayDocumentName = new NodeTranslator(nodeModel).getNodeName();
        titleModel = new StringResourceModel("schedule-publish-title", this, null, displayDocumentName);
        setCssClass("hippo-workflow-dialog");
        setFocusOnCancel();

        UnpublishedReferenceNodeProvider provider = new UnpublishedReferenceNodeProvider(
                new UnpublishedReferenceProvider(new ReferenceProvider(nodeModel)));
        add(new UnpublishedReferencesView("links", provider, editorMgr));

        addOrReplace(new DatePickerComponent(Dialog.BOTTOM_LEFT_ID, dateModel, new ResourceModel("schedule-publish-text")));
    }

    @Override
    public IModel<String> getTitle() {
        return titleModel;
    }

    @Override
    protected void onDetach() {
        if (titleModel != null) {
            titleModel.detach();
        }
        super.onDetach();
    }
}
