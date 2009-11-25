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

package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;

public abstract class AbstractXinhaDialog extends AbstractDialog<AbstractPersistedMap> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private boolean hasExistingLink;

    public AbstractXinhaDialog(IModel<AbstractPersistedMap> model) {
        super(model);

        hasExistingLink = model.getObject().isExisting();

        final Button remove = new AjaxButton("button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onRemoveLink();
                if (!hasError()) {
                    closeDialog();
                }
            }

            @Override
            public boolean isVisible() {
                return hasRemoveButton();
            }
        };
        remove.setModel(new ResourceModel("remove", "Remove"));
        addButton(remove);

    }

    public IModel<String> getTitle() {
        return new StringResourceModel("dialog-title", this, null);
    }

    protected void checkState() {
        IPersistedMap link = (IPersistedMap) getModelObject();
        enableOk(link.isValid() && link.hasChanged());
    }

    protected boolean hasRemoveButton() {
        return hasExistingLink;
    }

    //FIXME: remove and use set*Enabled
    protected void enableOk(boolean state) {
        setOkEnabled(state);
    }

    protected String getCancelScript() {
        return "if(openModalDialog != null){ openModalDialog.cancel(); }";
    }

    protected String getCloseScript() {
        String returnValue = ((IPersistedMap) getModelObject()).toJsString();
        return "if(openModalDialog != null){ openModalDialog.close(" + returnValue + "); }";
    }

    @Override
    public final void onClose() {
        onCloseInternal();

        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            String script;
            if (cancelled) {
                script = getCancelScript();
            } else {
                script = getCloseScript();
            }
            target.getHeaderResponse().renderOnDomReadyJavascript(script);
        }
        onCloseDialog();
    }

    void onCloseInternal() {
    }

    protected void onCloseDialog() {
    }

    protected void onRemoveLink() {
        IPersistedMap link = (IPersistedMap) getModelObject();
        link.delete();
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
