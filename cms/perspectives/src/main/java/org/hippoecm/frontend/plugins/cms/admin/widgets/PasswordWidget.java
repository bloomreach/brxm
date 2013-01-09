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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;

public class PasswordWidget extends AjaxUpdatingWidget {

    private static final long serialVersionUID = 1L;

    private final PasswordTextField field;
    private final Label label;

    /**
     * Create a password widget that is part of a html table
     * @param id
     * @param passwordModel
     * @param labelModel
     */
    public PasswordWidget(final String id, final IModel passwordModel, final IModel labelModel) {
        super(id, passwordModel);
        
        label = new Label("label", labelModel);
        add(label);
        
        field = new PasswordTextField("widget", (IModel<String>) this.getDefaultModel());
        field.setRequired(false);
        addFormField(field);
    }

    /**
     * Get the wicket password text field
     * @return
     */
    public PasswordTextField getPasswordTextField() {
        return field;
    }

    /**
     * @see org.apache.wicket.markup.html.form.PasswordTextField#setResetPassword(boolean)
     */
    public void setResetPassword(final boolean resetPassword) {
        field.setResetPassword(resetPassword);
    }

    /**
     * @see org.apache.wicket.markup.html.form.FormComponent#setRequired(boolean)
     */
    public void setRequired(final boolean required) {
        field.setRequired(required);
    }
    
    /**
     * @see org.apache.wicket.markup.html.form.FormComponent#add(IValidator)
     */
    public final FormComponent add(final IValidator validator) {
        return field.add(validator);
    }
}
