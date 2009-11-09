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

public abstract class AbstractXinhaDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public AbstractXinhaDialog(IModel model) {
        super(model);

        final Button remove = new AjaxButton("button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onRemove();
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

    public IModel getTitle() {
        return new StringResourceModel("dialog-title", this, null);
    }

    protected void checkState() {
        IPersistedMap link = (IPersistedMap) getModelObject();
        enableOk(link.isValid() && link.hasChanged());
    }

    protected boolean hasRemoveButton() {
        return ((IPersistedMap) getModelObject()).isExisting();
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

    @Override
    protected void onRemove() {
        IPersistedMap link = (IPersistedMap) getModelObject();
        link.delete();
        super.onRemove();
    }
    
    /**
     * Helper method for new PropertyModel on nested PersistedMap. Method getObjectClass is overridden
     * to help Wicket decide what type the nested model is.
     * 
     * @param _class  The type of the model's nested object
     * @param exp  Property expression  
     * 
     * @return A PropertyModel that wraps the Dialog's model.
     */
    protected IModel getPropertyModel(final Class<? extends AbstractPersistedMap> _class, String exp) {
        return new PropertyModel(getModel(), exp) {
            private static final long serialVersionUID = 1L;
            
            @Override
            public Class<? extends AbstractPersistedMap> getObjectClass() {
                return _class;
            }
        };
    }


}
