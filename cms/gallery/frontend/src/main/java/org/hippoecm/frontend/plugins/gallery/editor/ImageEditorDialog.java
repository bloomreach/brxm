package org.hippoecm.frontend.plugins.gallery.editor;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;

/**
 * ImageEditorDialog shows a modal dialog with simple image editor in it, the only operation available currently is
 * "cropping". The ImageEditorDialog will replace the current variant with the result of the image editing actions.
 */
public class ImageEditorDialog extends AbstractDialog {
    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("edit-image-dialog-title", ImageEditorDialog.this, null);
    }

    @Override
    protected void onOk() {
        super.onOk();    //TODO : replace the variant with the edited image.
    }


}

