/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.io.InputStream;
import java.util.Calendar;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropBehavior;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropSettings;
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
public class ImageCropEditorDialog extends AbstractDialog<Node> {

    Logger log = LoggerFactory.getLogger(ImageCropEditorDialog.class);

    @SuppressWarnings("unused")
    private String region;
    private GalleryProcessor galleryProcessor;
    private Dimension originalImageDimension;
    private Dimension configuredDimension;
    private Dimension thumbnailDimension;

    public ImageCropEditorDialog(IModel<Node> jcrImageNodeModel, GalleryProcessor galleryProcessor) {
        super(jcrImageNodeModel);

        this.galleryProcessor = galleryProcessor;
        Node thumbnailImageNode = jcrImageNodeModel.getObject();

        HiddenField<String> regionField = new HiddenField<>("region", new PropertyModel<String>(this, "region"));
        regionField.setOutputMarkupId(true);
        add(regionField);

        Component originalImage, imgPreview;
        try {
            Node originalImageNode = thumbnailImageNode.getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
            originalImageDimension = new Dimension(
                    (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getLong(),
                    (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getLong()
            );

            JcrNodeModel originalNodeModel = new JcrNodeModel(originalImageNode);
            originalImage = new JcrImage("image", new JcrResourceStream(originalNodeModel));
            imgPreview = new JcrImage("imagepreview", new JcrResourceStream(originalNodeModel));

        } catch (RepositoryException e) {
            log.error("Cannot retrieve original image", e);
            error(e);
            originalImage = new EmptyPanel("image");
            imgPreview = new EmptyPanel("imagepreview");
        }

        WebMarkupContainer imagePreviewContainer = new WebMarkupContainer("previewcontainer");
        imagePreviewContainer.setOutputMarkupId(true);
        try {
        	configuredDimension = galleryProcessor.getDesiredResourceDimension(thumbnailImageNode);
            thumbnailDimension = handleZeroValueInDimension(originalImageDimension, configuredDimension);

            imagePreviewContainer.add(new AttributeAppender("style", Model.of("height:" + thumbnailDimension.getHeight() + "px"), ";"));
            imagePreviewContainer.add(new AttributeAppender("style", Model.of("width:" + thumbnailDimension.getWidth() + "px"), ";"));

        } catch (RepositoryException | GalleryException e) {
            log.error("Cannot retrieve thumbnail dimensions", e);
            error(e);
        }

        boolean isUpscalingEnabled = true;
        try {
            isUpscalingEnabled = galleryProcessor.isUpscalingEnabled(thumbnailImageNode);
        } catch (GalleryException | RepositoryException e) {
            log.error("Cannot retrieve Upscaling configuration option", e);
            error(e);
        }

        Label thumbnailSize = new Label("thumbnail-size", new StringResourceModel("thumbnail-size", this, null));
        thumbnailSize.setOutputMarkupId(true);
        add(thumbnailSize);
               
        ImageCropSettings cropSettings = new ImageCropSettings(regionField.getMarkupId(),
                imagePreviewContainer.getMarkupId(),
                originalImageDimension,
                configuredDimension,
                thumbnailDimension,
                isUpscalingEnabled,
                thumbnailSize.getMarkupId(true));
        originalImage.add(new ImageCropBehavior(cropSettings));
        originalImage.setOutputMarkupId(true);

        add(originalImage);
        imgPreview.add(new AttributeAppender("style", Model.of("position:absolute"), ";"));
        imagePreviewContainer.add(imgPreview);
        imagePreviewContainer.setVisible(cropSettings.isPreviewVisible());
        add(imagePreviewContainer);

        add(new Label("preview-description", cropSettings.isPreviewVisible() ?
                new StringResourceModel("preview-description-enabled", this, null) :
                new StringResourceModel("preview-description-disabled", this, null))
        );
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
            Node originalImageNode = getModelObject().getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
            String mimeType = originalImageNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            ImageReader reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }
            ImageWriter writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }
            
            Binary binary = originalImageNode.getProperty(JcrConstants.JCR_DATA).getBinary();
            MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(binary.getStream());
            reader.setInput(imageInputStream);
            BufferedImage original = reader.read(0);
            Dimension thumbnailDimension = galleryProcessor.getDesiredResourceDimension(getModelObject());
            Dimension dimension = handleZeroValueInDimension(new Dimension(width, height), thumbnailDimension);
            Object hints;
            boolean highQuality;
            if (Math.min(width / reader.getWidth(0), height / reader.getHeight(0)) < 1.0) {
                hints = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                highQuality = true;
            } else {
                hints = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                highQuality = false;
            }
            BufferedImage thumbnail = ImageUtils.scaleImage(original, left, top, width, height, 
                    (int) dimension.getWidth(), (int) dimension.getHeight(), hints, highQuality);
            ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, thumbnail);

            Node cropped = getModelObject();

            InputStream is = new ByteArrayInputStream(bytes.toByteArray());
            Binary croppedBinary = ResourceHelper.getValueFactory(cropped).createBinary(is);
            cropped.getProperty(JcrConstants.JCR_DATA).setValue(croppedBinary);
            cropped.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
            cropped.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).setValue(dimension.getWidth());
            cropped.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).setValue(dimension.getHeight());

        } catch (GalleryException | IOException | RepositoryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        }
    }

    @Override
    protected boolean isFullscreenEnabled() {
        return true;

    }

    /**
     * If height or width in the thumbnailDimension is equal to 0 it is a special case. 
     * The value 0 represents a value that according to the dimension of the original image.
     *  
     * With this function a new dimension is created according to the original dimension
     * 
     * @param originalDimension dimension of the original image
     * @param thumbnailDimension dimension of the thumbnail image
     * @return scaled dimension based on width or height value
     */
    private Dimension handleZeroValueInDimension(Dimension originalDimension, Dimension thumbnailDimension) {
        Dimension normalized = new Dimension(thumbnailDimension);
        if(thumbnailDimension.height == 0) {            	
        	int height = (int) ((thumbnailDimension.getWidth() / originalDimension.getWidth()) * originalDimension.getHeight());             	
        	normalized.setSize(thumbnailDimension.width, height);
        }            
        if(thumbnailDimension.width == 0) {
        	int width = (int) ((thumbnailDimension.getHeight() / originalDimension.getHeight()) * originalDimension.getWidth());
            normalized.setSize(width, thumbnailDimension.height);
        }
        return normalized;    	
    }
}
