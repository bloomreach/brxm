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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnCloseDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(OnCloseDialog.class);

    public interface Actions {

        void save();

        void revert();

        void close();
    }

    public OnCloseDialog(final Actions actions, JcrNodeModel model, final IEditor editor) {
        super(model);

        setOkVisible(false);

        final Label exceptionLabel = new Label("exception", "");
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        AjaxButton button = new AjaxButton(getButtonId()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    actions.close();
                    actions.revert();
                    closeDialog();
                } catch (Exception ex) {
                    exceptionLabel.setModel(new Model(ex.getMessage()));
                    target.addComponent(exceptionLabel);
                }
            }
        };
        button.setModel(new ResourceModel("discard", "Discard"));
        addButton(button);

        button = new AjaxButton(getButtonId()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    actions.close();
                    actions.save();
                    closeDialog();
                } catch (Exception ex) {
                    exceptionLabel.setModel(new Model(ex.getMessage()));
                    target.addComponent(exceptionLabel);
                }
            }
        };
        button.setModel(new ResourceModel("save", "Save"));
        addButton(button);
    }

    public IModel getTitle() {
        return new StringResourceModel("close-document", this, null, new Object[] { new PropertyModel(getModel(),
        "name") }, "Close {0}");
    }

}
