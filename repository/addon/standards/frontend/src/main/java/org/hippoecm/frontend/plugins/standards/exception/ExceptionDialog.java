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
package org.hippoecm.frontend.plugins.standards.exception;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.ExceptionModel;

/**
 * @deprecated use org.hippoecm.frontend.sa.dialog.error.ErrorDialog instead
 */
public class ExceptionDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private String exception_message = "";
    @SuppressWarnings("unused")
    private String display_message = "";

    public ExceptionDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Exception");

        final Label exceptionMessageLabel = new Label("exception-message", new PropertyModel(this, "exception_message"));
        final Label displayMessageLabel = new Label("display-message", new PropertyModel(this, "display_message"));
        exceptionMessageLabel.setOutputMarkupId(true);
        add(exceptionMessageLabel);
        add(displayMessageLabel);

        cancel.setVisible(false);

        dialogWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
            }
        });
    }

    @Override
    protected void ok() throws Exception {
    }

    @Override
    protected void cancel() {
    }

    public void setExceptionMessage(ExceptionModel repositoryExceptionModel) {
        if(repositoryExceptionModel.getException() != null ) {
            this.exception_message = repositoryExceptionModel.getException().getMessage();
        }
        if(repositoryExceptionModel.getDisplayMessage() != null ) {
            this.display_message = repositoryExceptionModel.getDisplayMessage();
        }
    }

}
