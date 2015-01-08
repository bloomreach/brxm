/**
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.updater;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class LabelledTextAreaTableRow extends Panel {

    private static final long serialVersionUID = 1L;

    protected final Label label;
    protected final TextArea<String> textarea;

    public LabelledTextAreaTableRow(String id, final IModel<String> labelModel, final IModel<String> textareaModel) {
        super(id);
        label = new Label("label", labelModel);
        add(label);
        textarea = new TextArea<String>("textarea", textareaModel);
        add(textarea);
    }

}
