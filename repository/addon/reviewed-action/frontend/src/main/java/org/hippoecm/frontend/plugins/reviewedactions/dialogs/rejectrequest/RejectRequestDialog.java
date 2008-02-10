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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs.rejectrequest;

import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class RejectRequestDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private String reason;

    public RejectRequestDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        dialogWindow.setTitle("Reject request");

        add(new TextFieldWidget("reason", new PropertyModel(this, "reason")));
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void doOk() throws Exception {
        FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow();
        workflow.rejectRequest(reason);
    }

    @Override
    public void cancel() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
