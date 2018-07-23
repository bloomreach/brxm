/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

public abstract class AjaxWizardButton extends AjaxButton {
    private static final long serialVersionUID = 1L;


    private final IWizard wizard;

    public AjaxWizardButton(String id, IWizard wizard, final Form form, String labelResourceKey) {
        super(id, new ClassResourceModel(labelResourceKey, AjaxWizardButton.class), form);
        this.wizard = wizard;
        add(CssClass.append("btn btn-default"));
    }

    public AjaxWizardButton(String id, IWizard wizard, String labelResourceKey) {
        this(id, wizard, null, labelResourceKey);
    }

    protected final IWizard getWizard() {
        return wizard;
    }

    protected final IWizardModel getWizardModel() {
        return getWizard().getWizardModel();
    }

    @Override
    protected final void onSubmit(AjaxRequestTarget target, Form form) {
        onClick(target, form);
    }

    protected abstract void onClick(AjaxRequestTarget target, Form form);
}
