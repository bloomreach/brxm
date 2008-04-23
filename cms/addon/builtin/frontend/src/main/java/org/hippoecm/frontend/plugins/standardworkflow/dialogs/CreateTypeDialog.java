/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow.dialogs;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTypeDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(CreateTypeDialog.class);

    @SuppressWarnings("unused")
    private String name;

    public CreateTypeDialog(DialogWindow dialogWindow) {
        super(dialogWindow, "Create new type");

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
    }

    @Override
    protected void execute() throws Exception {
        RemodelWorkflow workflow = (RemodelWorkflow) getWorkflow();
        workflow.createType(name);

        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("flush", getDialogWindow().getNodeModel().getParentModel());
            channel.send(request);
        } else {
            log.error("could not send flush message");
        }
    }
}
