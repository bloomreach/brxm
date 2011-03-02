package org.hippoecm.frontend.plugins.gallery.editor;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import net.sf.json.JSONObject;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.gallery.editor.crop.CropBehavior;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImageEditorDialog shows a modal dialog with simple image editor in it, the only operation available currently is
 * "cropping". The ImageEditorDialog will replace the current variant with the result of the image editing actions.
 */
public class ImageEditorDialog extends AbstractDialog {

    Logger log = LoggerFactory.getLogger(ImageEditorDialog.class);

    private String region;
    private GalleryProcessor galleryProcessor;

    private ImageEditorDialog(){}

    public ImageEditorDialog(IModel<Node> jcrImageNodeModel, GalleryProcessor galleryProcessor){
        super(jcrImageNodeModel);
        this.galleryProcessor = galleryProcessor;

        TextField regionField = new TextField("region", new PropertyModel(this, "region"));
        regionField.setOutputMarkupId(true);
        add(regionField);

        if(getModelObject() != null){
            try {
                Node originalImageNode = ((Node) getModelObject()).getParent().getNode("hippogallery:original");
                JcrNodeModel nodeModel = new JcrNodeModel(originalImageNode);

                JcrImage originalImage = new JcrImage("image", new JcrResourceStream(nodeModel));
                JcrImage imgPreview = new JcrImage("imagepreview", new JcrResourceStream(nodeModel));
                WebMarkupContainer imagePreviewContainer = new WebMarkupContainer("previewcontainer");
                imagePreviewContainer.setOutputMarkupId(true);
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("border:1px solid black"), ";"));
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("height:83px"), ";"));
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("width:125px"), ";"));
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("position:relative"), ";"));
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("overflow:hidden"), ";"));

                imgPreview.add(new AttributeAppender("style", new Model<String>("top:-20px"), ";"));
                imgPreview.add(new AttributeAppender("style", new Model<String>("left:-20px"), ";"));
                imgPreview.add(new AttributeAppender("style", new Model<String>("position:absolute"), ";"));

                imagePreviewContainer.add(imgPreview);

                originalImage.add(new CropBehavior(regionField.getMarkupId(), imagePreviewContainer.getMarkupId()));

                add(originalImage);
                add(imagePreviewContainer);

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
        JSONObject jsonObject = JSONObject.fromObject(region);
        int top = jsonObject.getInt("top");
        int height = jsonObject.getInt("height");
        int left = jsonObject.getInt("left");
        int width = jsonObject.getInt("width");
        try {
            Node originalImageNode = ((Node)getModelObject()).getParent().getNode("hippogallery:original");
            String mimeType = originalImageNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            ImageReader reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }
            ImageWriter writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }
            JcrNodeModel nodeModel = new JcrNodeModel(originalImageNode);
            MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(originalImageNode.getProperty(JcrConstants.JCR_DATA).getStream());
            reader.setInput(imageInputStream);
            BufferedImage original = reader.read(0);
            Dimension dimension = galleryProcessor.getDesiredResourceDimension((Node)getModelObject());
            BufferedImage thumbnail = ImageUtils.scaleImage(original, left, top, width, height, (int)dimension.getWidth(), (int)dimension.getHeight(), null, true);
            ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, thumbnail);
            ((Node)getModelObject()).getProperty(JcrConstants.JCR_DATA).setValue(new ByteArrayInputStream(bytes.toByteArray()));
        } catch (GalleryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        } catch (IOException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        } catch (RepositoryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        }
    }
}

