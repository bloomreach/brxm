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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDialog extends Panel implements IDialogService.Dialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    protected AjaxLink ok;
    protected AjaxLink cancel;
    protected WicketAjaxIndicatorAppender indicator;
    private IDialogService dialogService;

    private String exception = "";

    public AbstractDialog(IDialogService dialogWindow) {
        super(IDialogService.DIALOG_WICKET_ID);

        this.dialogService = dialogWindow;

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

        ok = new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
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
        add(ok);

        cancel = new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancel();
                closeDialog();
            }
        };
        add(cancel);
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
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
