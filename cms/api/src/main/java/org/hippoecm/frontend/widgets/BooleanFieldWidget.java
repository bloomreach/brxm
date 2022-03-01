/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;

public class BooleanFieldWidget extends AjaxUpdatingWidget<Boolean> {

    public BooleanFieldWidget(String id, IModel<Boolean> model) {
        super(id, model);
        addFormField(new AjaxCheckBox("widget", model) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                BooleanFieldWidget.this.onUpdate(target);
            }
        });
    }
}
