/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.ckeditor;

import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.hippoecm.frontend.plugins.richtext.dialog.RichTextEditorAction;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;

/**
 * Executes the CKEditor command to insert a picked internal link into an editor.
 */
class CKEditorInsertInternalLinkAction implements RichTextEditorAction<RichTextEditorDocumentLink> {

    private final String editorId;

    CKEditorInsertInternalLinkAction(final String editorId) {
        this.editorId = editorId;
    }

    @Override
    public String getJavaScript(final RichTextEditorDocumentLink documentLink) {
        return "CKEDITOR.instances." + editorId + ".execCommand('" + HippoPicker.InternalLink.COMMAND_INSERT_INTERNAL_LINK + "', " + documentLink.toJsString() + ");";
    }

}
