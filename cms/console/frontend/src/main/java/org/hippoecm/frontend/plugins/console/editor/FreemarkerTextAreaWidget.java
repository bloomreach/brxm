/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.codemirror.CodeMirrorEditor;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Freemarker template editing {@link TextAreaWidget}.
 */
public class FreemarkerTextAreaWidget extends CodeMirrorTextAreaWidget {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FreemarkerTextAreaWidget.class);

    public FreemarkerTextAreaWidget(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected CodeMirrorEditor createEditor(final IModel<String> model) {
        return new FreemarkerCodeMirrorEditor("widget", "ftl-code-editor", model);
    }

    private class FreemarkerCodeMirrorEditor extends CodeMirrorEditor {

        private static final long serialVersionUID = 1L;

        {
            setFlag(FLAG_CONVERT_EMPTY_INPUT_STRING_TO_NULL, false);
        }

        public FreemarkerCodeMirrorEditor(String id, String editorName, IModel<String> model) {
            super(id, editorName, model);
            setEditorMode(MIME_TYPE_HTML);
            setChangeEventTriggeringEnabled(true);
        }

        @Override
        protected void renderHeadForCustomModes(final IHeaderResponse response) {
            response.render(JavaScriptHeaderItem.forReference(XML_MODE_JS));
            response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT_MODE_JS));
            response.render(JavaScriptHeaderItem.forReference(CSS_MODE_JS));
            response.render(JavaScriptHeaderItem.forReference(HTML_MIXED_MODE_JS));
        }
    }

}
