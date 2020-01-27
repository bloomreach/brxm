/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;

public class FieldTitle extends Panel {

    protected WebMarkupContainer h3;

    public FieldTitle(final String id, final IModel<String> labelModel, final IModel<String> hintModel, final boolean required) {
        super(id);

        h3 = new WebMarkupContainer("h3");
        h3.setOutputMarkupPlaceholderTag(true);
        add(h3);

        h3.add(new Label("name", labelModel));

        final Label requiredLabel = new Label("required", "*");
        requiredLabel.setVisible(required);
        h3.add(requiredLabel);

        add(new FieldHint("hint-panel", hintModel));
    }
}
