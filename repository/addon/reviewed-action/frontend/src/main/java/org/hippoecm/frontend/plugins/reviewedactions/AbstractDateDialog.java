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
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;

public abstract class AbstractDateDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected Date date;

    protected AjaxLink now;

    public AbstractDateDialog(AbstractWorkflowPlugin workflowPlugin, IModel title, IModel question, Date date) {
        super(workflowPlugin, title);
        this.date = date;

        add(new Label("question", question));

        add(new AjaxDateTimeField("value", new PropertyModel(this, "date")));

        now = new AjaxLink("now") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
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
                    setException(msg);
                    target.addComponent(exceptionLabel);
                    e.printStackTrace();
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new IAjaxCallDecorator() {
                    private static final long serialVersionUID = 1L;

                    public CharSequence decorateOnFailureScript(CharSequence script) {
                        return getScript("none") + script;
                    }

                    public CharSequence decorateOnSuccessScript(CharSequence script) {
                        return getScript("none") + script;
                    }

                    public CharSequence decorateScript(CharSequence script) {
                        return getScript("block") + script;
                    }

                    private String getScript(String state) {
                        String id = indicator.getMarkupId();
                        return "document.getElementById('" + id + "').style.display = '" + state + "';";
                    }
                };
            }
        };
        add(now);
    }
}
