/**
 * Copyright (C) 2012 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.dev.updater;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

public class LabelledInputFieldTableRow extends Panel {

    protected final Label label;
    protected final TextField<String> input;

    public LabelledInputFieldTableRow(String id, final IModel<String> labelModel, final IModel<String> inputModel) {
        super(id);
        label = new Label("label", labelModel);
        add(label);
        input = new TextField<String>("input", inputModel);
        add(input);
    }

}
