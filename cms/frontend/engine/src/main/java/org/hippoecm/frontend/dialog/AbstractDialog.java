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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDialog extends Panel implements IDialogService.Dialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    protected AjaxButton ok;
    protected AjaxButton cancel;
    protected WicketAjaxIndicatorAppender indicator;
    private IDialogService dialogService;
    private Form form;

    private String exception = "";

    public AbstractDialog() {
        super(IDialogService.DIALOG_WICKET_ID);

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        add(indicator = new WicketAjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpanClass() {
                return "wicket-ajax-indicator-dialog";
            }
        });

        form = new Form("form") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isTransparentResolver() {
                return true;
            }
        };
        form.setOutputMarkupId(true);
        add(form);

        ok = new AjaxButton("ok", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
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
        form.add(ok);

        cancel = new AjaxButton("cancel", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                cancel();
                closeDialog();
            }
        };
        form.add(cancel);
    }

    public void add(FormComponent formComponent) {
        form.add(formComponent);
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }

    public void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    protected IDialogService getDialogService() {
        return dialogService;
    }

    protected final void closeDialog() {
        getDialogService().close();
        onClose();
    }

    protected void ok() throws Exception {
    }

    protected void cancel() {
    }

    public Component getComponent() {
        return this;
    }

    public void onClose() {
    }

}
