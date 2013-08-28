package org.hippoecm.frontend.plugins.ckeditor;

import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.hippoecm.frontend.plugins.richtext.dialog.RichTextEditorAction;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;

/**
 * Executes the CKEditor command to insert a picked image into an editor.
 */
class CKEditorInsertImageAction implements RichTextEditorAction<RichTextEditorImageLink> {

    private final String editorId;

    CKEditorInsertImageAction(final String editorId) {
        this.editorId = editorId;
    }

    @Override
    public String getJavaScript(final RichTextEditorImageLink imageLink) {
        return "CKEDITOR.instances." + editorId + ".execCommand('" + HippoPicker.Image.COMMAND_INSERT_IMAGE + "', " + imageLink.toJsString() + ");";
    }

}
