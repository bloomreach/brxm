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

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugins.cms.dev.codemirror.CodeMirrorEditor;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CodeMirror editing {@link TextAreaWidget}.
 */
public class CodeMirrorTextAreaWidget extends TextAreaWidget {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CodeMirrorTextAreaWidget.class);

    private static final CssReferenceHeaderItem EDITOR_CSS = CssHeaderItem.forReference(new CssResourceReference(CodeMirrorTextAreaWidget.class, "CodeMirrorTextAreaWidget.css"));

    public CodeMirrorTextAreaWidget(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected TextArea<String> createTextArea(final IModel<String> model) {
        CodeMirrorEditor editor = createEditor(model);
        return editor;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(EDITOR_CSS);
    }

    /**
     * Creates {@link CodeMirrorEditor} instance.
     * @param model
     * @return
     */
    protected CodeMirrorEditor createEditor(final IModel<String> model) {
        CodeMirrorEditor editor = new CodeMirrorEditor("widget", "code-editor", model);
        editor.setChangeEventTriggeringEnabled(true);
        return editor;
    }

}
