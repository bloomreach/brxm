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
