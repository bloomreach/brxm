/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.HTML5Attributes;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.OnEnterAjaxBehavior;

public class SubmittingTextField extends TextField<String> implements IFormSubmittingComponent {

    public SubmittingTextField(final String id, final IModel<String> model) {
        super(id, model);

        add(new HTML5Attributes());

        add(new OnEnterAjaxBehavior() {

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
               onEnter(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        });
    }

    public void onEnter(final AjaxRequestTarget target) {
    }

    @Override
    public Component setDefaultFormProcessing(final boolean defaultFormProcessing) {
        return this;
    }

    @Override
    public boolean getDefaultFormProcessing() {
        return true;
    }

    @Override
    public void onSubmit() {
    }

    @Override
    public void onAfterSubmit() {
    }

    @Override
    public void onError() {
    }
}
