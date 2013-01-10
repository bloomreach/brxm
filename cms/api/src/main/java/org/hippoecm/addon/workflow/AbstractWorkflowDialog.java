/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWorkflowDialog<T> extends AbstractDialog<T> {

    private static Logger log = LoggerFactory.getLogger(AbstractWorkflowDialog.class);

    private final IWorkflowInvoker invoker;

    public AbstractWorkflowDialog(IModel<T> model, IWorkflowInvoker invoker) {
        this(model, null, invoker);
    }

    public AbstractWorkflowDialog(IModel<T> model, IModel message, IWorkflowInvoker invoker) {
        super(model);
        this.invoker = invoker;

        Label notification = new Label("notification");
        if (message != null) {
            notification.setDefaultModel(message);
        } else {
            notification.setVisible(false);
        }
        add(notification);
        notification.add(new AttributeAppender("class", Model.of("notification"), " "));
    }

    @Override
    protected void onOk() {
        try {
            invoker.invokeWorkflow();
        } catch (Exception e) {
            log.error("Could not execute workflow.", e);
            error(e);
        }
    }
}
