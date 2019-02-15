/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowDialog<T> extends Dialog<T> {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDialog.class);

    private final IWorkflowInvoker invoker;
    private final IModel<String> titleModel;
    private final Label notification;

    public WorkflowDialog(final IWorkflowInvoker invoker) {
        this(invoker, null);
    }

    public WorkflowDialog(final IWorkflowInvoker invoker, final IModel<T> model) {
        this(invoker, model, null);
    }

    public WorkflowDialog(final IWorkflowInvoker invoker, final IModel<T> model,
                          final IModel<String> titleModel) {
        super(model);

        this.titleModel = titleModel;
        this.invoker = invoker;

        setCssClass("hippo-workflow-dialog");

        notification = new Label("notification");
        notification.add(CssClass.append("notification"));
        // Hide notification label by default until a model is set by calling #setNotification
        notification.setVisible(false);
        add(notification);
    }

    @Override
    public IModel<String> getTitle() {
        if (titleModel != null) {
            return titleModel;
        }
        return super.getTitle();
    }

    public void setNotification(final IModel<String> notificationModel) {
        notification.setDefaultModel(notificationModel);
        notification.setVisible(true);
    }

    @Override
    protected void onOk() {
        try {
            invoker.invokeWorkflow();
        } catch (final WorkflowException e) {
            log.warn("Could not execute workflow: " + e.getMessage());
            error(e);
        } catch (final Exception e) {
            log.error("Could not execute workflow.", e);
            error(e);
        }
    }

    @Override
    protected void onDetach() {
        if (titleModel != null) {
            titleModel.detach();
        }
        super.onDetach();
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(final String id) {
        return new FeedbackPanel(id);
    }

    protected IWorkflowInvoker getInvoker() {
        return invoker;
    }
}
