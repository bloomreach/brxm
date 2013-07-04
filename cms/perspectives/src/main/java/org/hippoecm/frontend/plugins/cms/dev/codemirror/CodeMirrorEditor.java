/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dev.codemirror;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * Component that displays a CodeMirror panel which gives a nice syntax highlighting for Groovy.
 */
public class CodeMirrorEditor extends TextArea<String> {

    private static final JavaScriptResourceReference CODEMIRROR_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "lib/codemirror.js");
    private static final JavaScriptResourceReference GROOVY_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/groovy/groovy.js");
    private static final CssResourceReference CODEMIRROR_SKIN = new CssResourceReference(CodeMirrorEditor.class, "lib/codemirror.css");
    private static final CssResourceReference ECLIPSE_SKIN = new CssResourceReference(CodeMirrorEditor.class, "theme/eclipse.css");
    private String markupId;
    private String editorName;

    public CodeMirrorEditor(final String id, final String editorName, final IModel<String> model) {
        super(id, model);
        this.editorName = editorName;
        setOutputMarkupId(true);
        markupId = getMarkupId();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(CODEMIRROR_JS));
        response.render(JavaScriptHeaderItem.forReference(GROOVY_JS));
        response.render(CssHeaderItem.forReference(CODEMIRROR_SKIN));
        response.render(CssHeaderItem.forReference(ECLIPSE_SKIN));
        response.render(OnLoadHeaderItem.forScript(getJavaScriptForEditor()));
    }

    private String getJavaScriptForEditor() {
        return "var cm = CodeMirror.fromTextArea(document.getElementById('" + markupId + "'), " +
                "{lineNumbers: true, matchBrackets: true, mode: \"text/x-groovy\", " +
                "onChange: function(cm) { cm.save(); }, editorName: \"" + editorName + "\"});\n";
    }
}
