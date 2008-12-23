/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Date;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;

public abstract class AbstractDateDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected Date date;

    protected Button now;

    public AbstractDateDialog(AbstractWorkflowPlugin workflowPlugin, IModel question, Date date) {
        super(workflowPlugin);
        this.date = date;

        add(new Label("question", question));

        add(new AjaxDateTimeField("value", new PropertyModel(this, "date")));

        now = new AjaxButton(getButtonId()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                AbstractDateDialog.this.date = null;
                try {
                    ok();
                    closeDialog();
                } catch (Exception e) {
                    String msg = e.getClass().getName() + ": " + e.getMessage();
                    log.error(msg);
                    if (log.isDebugEnabled()) {
                        log.debug("Error from repository: ", e);
                    }
                    error(msg);
                    e.printStackTrace();
                }
            }
        };
        now.add(new Label("label", new ResourceModel("now")));
        addButton(now);
    }
}
