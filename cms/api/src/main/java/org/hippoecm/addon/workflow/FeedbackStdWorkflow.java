/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.addon.workflow;

import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.StdWorkflow;

public class FeedbackStdWorkflow extends StdWorkflow {

    private final IModel<Boolean> visibilityModel;
    private final IModel<String> messageModel;

    public FeedbackStdWorkflow(final String id, final IModel<String> messageModel, final IModel<Boolean> visibilityModel) {
        super("info" + id, messageModel);
        this.visibilityModel = visibilityModel;
        this.messageModel = messageModel;
    }

    @Override
    public String getSubMenu() {
        return "info";
    }

    @Override
    protected IModel<String> getTitle() {
        return messageModel;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        visibilityModel.detach();
        messageModel.detach();
    }
}
