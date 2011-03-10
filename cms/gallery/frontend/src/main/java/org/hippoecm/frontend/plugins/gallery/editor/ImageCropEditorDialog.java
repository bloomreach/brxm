/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.gallery.editor;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.gallery.editor.crop.CropBehavior;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

/**
 * ImageCropEditorDialog shows a modal dialog with simple image editor in it, the only operation available currently is
 * "cropping". The ImageCropEditorDialog will replace the current variant with the result of the image editing actions.
 */
public class ImageCropEditorDialog extends AbstractDialog {

    Logger log = LoggerFactory.getLogger(ImageCropEditorDialog.class);

    private String region;
    private GalleryProcessor galleryProcessor;
    private Dimension originalImageDimension;
    private Dimension thumbnailDimension;

    private ImageCropEditorDialog(){}

    public ImageCropEditorDialog(IModel<Node> jcrImageNodeModel, GalleryProcessor galleryProcessor){
        super(jcrImageNodeModel);
        this.galleryProcessor = galleryProcessor;

        HiddenField regionField = new HiddenField("region", new PropertyModel(this, "region"));
        regionField.setOutputMarkupId(true);
        add(regionField);


        if(getModelObject() != null){
            try {
                Node thumbnailImageNode = (Node) jcrImageNodeModel.getObject();
                thumbnailDimension = galleryProcessor.getDesiredResourceDimension(thumbnailImageNode);
                Node originalImageNode = thumbnailImageNode.getParent().getNode("hippogallery:original");
                originalImageDimension = new Dimension(
                                        (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getLong(),
                                        (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getLong());

                JcrNodeModel originalNodeModel = new JcrNodeModel(originalImageNode);
                JcrImage originalImage = new JcrImage("image", new JcrResourceStream(originalNodeModel));

                boolean isPreviewVisible = thumbnailDimension.getWidth() <= 200;

                JcrImage imgPreview = new JcrImage("imagepreview", new JcrResourceStream(originalNodeModel));
                imgPreview.add(new AttributeAppender("style", new Model<String>("position:absolute"), ";"));

                WebMarkupContainer imagePreviewContainer = new WebMarkupContainer("previewcontainer");
                imagePreviewContainer.setOutputMarkupId(true);
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("height:"+ thumbnailDimension.getHeight() +"px"), ";"));
                imagePreviewContainer.add(new AttributeAppender("style", new Model<String>("width:"+ thumbnailDimension.getWidth() +"px"), ";"));
                imagePreviewContainer.add(imgPreview);
                imagePreviewContainer.setVisible(isPreviewVisible);

                originalImage.add(new CropBehavior(
                    regionField.getMarkupId(),
                    imagePreviewContainer.getMarkupId(),
                    originalImageDimension,
                    thumbnailDimension));

                add(originalImage);
                add(imagePreviewContainer);

                Label previewDescription = new Label("preview-description", isPreviewVisible ?
                                new StringResourceModel("preview-description-enabled", this, null) :
                                new StringResourceModel("preview-description-disabled", this, null));
                add(previewDescription);

                Label thumbnailSize = new Label("thumbnail-size", new StringResourceModel("thumbnail-size", this, null, new Object[]{
                        thumbnailImageNode.getName(), ((int)thumbnailDimension.getWidth()) + " x " + ((int) thumbnailDimension.getHeight())
                }));
                add(thumbnailSize);


            } catch (RepositoryException e) {
                log.error(e.getMessage());
                error(e);
            } catch (GalleryException e){
                log.error(e.getMessage());
                error(e);
            }
        }
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=855,height=565").makeImmutable();
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("edit-image-dialog-title", ImageCropEditorDialog.this, null);
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
            MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(originalImageNode.getProperty(JcrConstants.JCR_DATA).getStream());
            reader.setInput(imageInputStream);
            BufferedImage original = reader.read(0);
            Dimension dimension = galleryProcessor.getDesiredResourceDimension((Node)getModelObject());
            Object hints;
            boolean highQuality;
            if(Math.min(width / reader.getWidth(0), height / reader.getHeight(0)) < 1.0) {
                hints = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                highQuality = true;
            } else {
                hints = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                highQuality = false;
            }
            BufferedImage thumbnail = ImageUtils.scaleImage(original, left, top, width, height, (int)dimension.getWidth(), (int)dimension.getHeight(), hints, highQuality);
            ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, thumbnail);

            Node modelObject = (Node) getModelObject();
            modelObject.getProperty(JcrConstants.JCR_DATA).setValue(new ByteArrayInputStream(bytes.toByteArray()));
            modelObject.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
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
