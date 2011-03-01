package org.hippoecm.frontend.plugins.gallery.editor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugins.gallery.editor.crop.CropBehavior;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.resource.JcrResourceStream;

/**
 * ImageEditorDialog shows a modal dialog with simple image editor in it, the only operation available currently is
 * "cropping". The ImageEditorDialog will replace the current variant with the result of the image editing actions.
 */
public class ImageEditorDialog extends AbstractDialog {

    private String region;

    private ImageEditorDialog(){}

    public ImageEditorDialog(IModel<Node> jcrImageNodeModel){
        super(jcrImageNodeModel);

        TextField regionField = new TextField("region", new PropertyModel(this, "region"));
        regionField.setOutputMarkupId(true);
        add(regionField);

        if(getModelObject() != null){
            try {
                Node originalImageNode = ((Node) getModelObject()).getParent().getNode("hippogallery:original");
                JcrNodeModel nodeModel = new JcrNodeModel(originalImageNode);
                JcrImage img = new JcrImage("image", new JcrResourceStream(nodeModel));
                img.add(new CropBehavior(regionField.getMarkupId()));
                add(img);

            } catch (RepositoryException e) {
                // FIXME: report back to user
                e.printStackTrace();
            }
        }
    }



    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("edit-image-dialog-title", ImageEditorDialog.this, null);
    }

    @Override
    protected void onOk() {
        super.onOk();    //TODO : replace the variant with the edited image.
        System.out.println(region);

    }


}

