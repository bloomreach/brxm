/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugins.richtext.model.IPersisted;

public abstract class AbstractXinhaDialog<T extends IPersisted> extends AbstractDialog<T> {
    private static final long serialVersionUID = 1L;


    private boolean hasExistingLink;

    public AbstractXinhaDialog(IModel<T> model) {
        super(model);

        hasExistingLink = getModelObject().isExisting();

        addButton(new AjaxButton("button", new ResourceModel("remove", "Remove")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onRemoveLink();
                if (!hasError()) {
                    closeDialog();
                }
            }

            @Override
            public boolean isVisible() {
                return hasRemoveButton();
            }
        });
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("dialog-title", this, null);
    }

    protected void checkState() {
        setOkEnabled(getModelObject().isValid() && getModelObject().hasChanged());
    }

    protected boolean hasRemoveButton() {
        return hasExistingLink;
    }

    protected String getCancelScript() {
        return "if(ModalDialog.current != null){ ModalDialog.current.cancel(); }";
    }

    protected String getCloseScript() {
        return "if(ModalDialog.current != null){ ModalDialog.current.close(" + getModelObject().toJsString() + "); }";
    }

    @Override
    public final void onClose() {
        onCloseInternal();

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.getHeaderResponse().render(OnDomReadyHeaderItem.forScript(cancelled ? getCancelScript() : getCloseScript()));
        }
        onCloseDialog();

        super.onClose();
    }

    void onCloseInternal() {
    }

    protected void onCloseDialog() {
    }

    protected void onRemoveLink() {
        getModelObject().delete();
    }
    
    protected static class StringPropertyModel extends PropertyModel<String> {
        private static final long serialVersionUID = 1L;

        public StringPropertyModel(Object modelObject, String expression) {
            super(modelObject, expression);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class getObjectClass() {
            return String.class;
        }
    }

}
