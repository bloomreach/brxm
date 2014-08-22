/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.widgets.TextAreaWidget;

/**
 * Freemarker template source string property value editor with CodeMirrorEditor support
 */
public class FreemarkerCodeMirrorPropertyValueEditor extends CodeMirrorPropertyValueEditor {

    private static final long serialVersionUID = 1L;

    FreemarkerCodeMirrorPropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
    }

    @Override
    protected TextAreaWidget createEditorWidget(String id, IModel<String> model) {
        return new FreemarkerTextAreaWidget(id, model);
    }
}
