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

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

/**
 * Component that displays a CodeMirror panel which gives a nice syntax highlighting for Groovy.
 */
public class CodeMirrorEditor extends TextArea<String> {

    private String markupId;
    private String editorName;

    public CodeMirrorEditor(final String id, final String editorName, final IModel<String> model) {
        super(id, model);
        this.editorName = editorName;
        setOutputMarkupId(true);
        markupId = getMarkupId();
        add(JavascriptPackageResource.getHeaderContribution(CodeMirrorEditor.class, "lib/codemirror.js"));
        add(CSSPackageResource.getHeaderContribution(CodeMirrorEditor.class, "lib/codemirror.css"));
        add(CSSPackageResource.getHeaderContribution(CodeMirrorEditor.class, "theme/eclipse.css"));
        add(JavascriptPackageResource.getHeaderContribution(CodeMirrorEditor.class, "mode/groovy/groovy.js"));

        add(new AbstractBehavior() {
            @Override
            public void renderHead(IHeaderResponse response) {
                response.renderOnLoadJavascript(getJavaScriptForEditor());
            }
        });
    }

    private String getJavaScriptForEditor() {
        return "var cm = CodeMirror.fromTextArea(document.getElementById('" + markupId + "'), " +
                "{lineNumbers: true, matchBrackets: true, mode: \"text/x-groovy\", " +
                "onChange: function(cm) { cm.save(); }, editorName: \"" + editorName + "\"});\n";
    }
}
