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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs.requestdepublication;

import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class RequestDePublicationDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    public RequestDePublicationDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Request de-publication");
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void doOk() throws Exception {
        BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
        workflow.requestDepublication();
    }

    @Override
    public void cancel() {
    }

}
