/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.widgets.TextAreaWidget;

/**
 * String property value editor with CodeMirrorEditor support
 */
public class CodeMirrorPropertyValueEditor extends PropertyValueEditor {

    private static final int MIN_ROWS = 10;

    CodeMirrorPropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
    }

    @Override
    protected Component createValueEditor(final JcrPropertyValueModel valueModel) {
        StringConverter stringModel = new StringConverter(valueModel);
        TextAreaWidget editor = createEditorWidget("value", stringModel);
        editor.setRows(String.valueOf(getRows(stringModel)));
        return editor;
    }

    /**
     * Creates {@link TextAreaWidget} instance where CodeMirror editor should be placed.
     * @param id
     * @param model
     * @return
     */
    protected TextAreaWidget createEditorWidget(String id, IModel<String> model) {
        return new CodeMirrorTextAreaWidget(id, model);
    }

    /**
     * Returns the <code>rows</code> attribute value for the editor text area.
     * @return
     */
    protected int getRows(IModel<String> model) {
        String asString = model.getObject();
        return Math.max(MIN_ROWS, asString.length() / 80);
    }
}
