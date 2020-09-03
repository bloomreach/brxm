/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.form;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public class PostOnlyForm<T> extends Form<T> {
    public PostOnlyForm(final String id) {
        super(id);
    }

    public PostOnlyForm(final String id, final IModel<T> model) {
        super(id, model);
    }

    @Override
    protected MethodMismatchResponse onMethodMismatch() {
        return MethodMismatchResponse.ABORT;
    }

    @Override
    protected String getMethod() {
        return METHOD_POST;
    }
}
