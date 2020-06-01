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
package org.hippoecm.frontend.plugins.cms.codemirror;

import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * Component that displays a CodeMirror panel which gives a nice syntax highlighting for Groovy, etc.
 * The default language mode is groovy.
 * You can override {@link #renderHeadForCustomModes(IHeaderResponse)} and {@link #getEditorMode()}
 * to use other language mode.
 */
public class CodeMirrorEditor extends TextArea<String> {

    private static final long serialVersionUID = 1L;

    /** A mime type of CodeMirror Groovy Editor Mode */
    public static final String MIME_TYPE_GROOVY = "text/x-groovy";

    /** A mime type of CodeMirror XML Editor Mode */
    public static final String MIME_TYPE_XML = "application/xml";

    /** A mime type of CodeMirror JavaScript Editor Mode */
    public static final String MIME_TYPE_JAVASCRIPT = "text/javascript";

    /** A mime type of CodeMirror CSS Editor Mode */
    public static final String MIME_TYPE_CSS = "text/css";

    /** A mime type of CodeMirror HTMLMIXED Editor Mode */
    public static final String MIME_TYPE_HTML = "text/html";

    /** Built-in Groovy CodeMirror mode */
    public static final JavaScriptResourceReference GROOVY_MODE_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/groovy/groovy.js");

    /** Built-in XML CodeMirror mode */
    public static final JavaScriptResourceReference XML_MODE_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/xml/xml.js");

    /** Built-in JavaScript CodeMirror mode */
    public static final JavaScriptResourceReference JAVASCRIPT_MODE_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/javascript/javascript.js");

    /** Built-in CSS CodeMirror mode */
    public static final JavaScriptResourceReference CSS_MODE_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/css/css.js");

    /** Built-in HTML MIXED CodeMirror mode */
    public static final JavaScriptResourceReference HTML_MIXED_MODE_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "mode/htmlmixed/htmlmixed.js");

    /** CodeMirror skin */
    private static final CssResourceReference CODEMIRROR_SKIN = new CssResourceReference(CodeMirrorEditor.class, "lib/codemirror.css");

    /** CodeMirror JavaScript */
    private static final JavaScriptResourceReference CODEMIRROR_JS = new JavaScriptResourceReference(CodeMirrorEditor.class, "lib/codemirror.js");

    /** Built-in Eclipse CodeMirror skin */
    private static final CssResourceReference ECLIPSE_SKIN = new CssResourceReference(CodeMirrorEditor.class, "theme/eclipse.css");

    /** CodeMirror instantiation script */
    public static final String CREATE_EDITOR_SCRIPT = "create-codemirror-editor.js";

    private String editorName;
    private String editorMode = MIME_TYPE_GROOVY;
    private boolean readOnly;
    private boolean changeEventTriggeringEnabled;

    public CodeMirrorEditor(final String id, final String editorName) {
        super(id);
        this.editorName = editorName;
        setOutputMarkupId(true);
    }

    public CodeMirrorEditor(final String id, final String editorName, final IModel<String> model) {
        super(id, model);
        this.editorName = editorName;
        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CODEMIRROR_SKIN));
        renderHeadForCustomSkins(response);
        response.render(JavaScriptHeaderItem.forReference(CODEMIRROR_JS));
        renderHeadForCustomModes(response);
        response.render(OnLoadHeaderItem.forScript(getJavaScriptForEditor()));
    }

    /**
     * Returns the CodeMirror editor name (in JavaScript context).
     * @return
     */
    public String getEditorName() {
        return editorName;
    }

    /**
     * Returns the MIME Type to be associated with a CodeMirror editor mode.
     * By default, it returns {@link #MIME_TYPE_GROOVY}.
     * @return
     */
    public String getEditorMode() {
        return editorMode;
    }

    public void setEditorMode(String editorMode) {
        this.editorMode = editorMode;
    }

    /**
     * Returns true if the editor should be read-only.
     * @return
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Flags whether or not the <code>onchange</code> event should be triggered to
     * the original (TextArea) element.
     * <P>
     * In a normal html form where users are required to click on save or submit button,
     * this option is not necessary.
     * However, if you have to capture <code>onchange</code> event to update server-side status
     * through AJAX code like Wicket AJAX component does, then you'll probably need to turn this on.
     * </P>
     * @return
     */
    public boolean isChangeEventTriggeringEnabled() {
        return changeEventTriggeringEnabled;
    }

    public void setChangeEventTriggeringEnabled(boolean changeEventTriggeringEnabled) {
        this.changeEventTriggeringEnabled = changeEventTriggeringEnabled;
    }

    /**
     * Renders head elements for custom CodeMirror skins.
     * By default, it renders {@link #ECLIPSE_SKIN}.
     * @param response
     */
    protected void renderHeadForCustomSkins(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(ECLIPSE_SKIN));
    }

    /**
     * Renders head elements for custom mode.
     * By default, it renders {@link #GROOVY_MODE_JS}.
     * @param response
     */
    protected void renderHeadForCustomModes(final IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(GROOVY_MODE_JS));
    }

    /**
     * Returns CodeMirror instance loading JavaScript statements.
     * @return
     */
    protected String getJavaScriptForEditor() {
        final Map<String, String> scriptParams = new TreeMap<>();
        scriptParams.put("markupId", getMarkupId());
        scriptParams.put("editorMode", getEditorMode());
        scriptParams.put("editorName", getEditorName());
        scriptParams.put("readOnly", Boolean.toString(isReadOnly()));
        scriptParams.put("changeEventTriggerEnabled", Boolean.toString(isChangeEventTriggeringEnabled()));

        final PackageTextTemplate script = new PackageTextTemplate(CodeMirrorEditor.class, CREATE_EDITOR_SCRIPT);
        return script.asString(scriptParams);
    }
}
