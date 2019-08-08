/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.awt.Rectangle;
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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.attributes.StyleAttribute;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropBehavior;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropSettings;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScaleImageOperation;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.util.ImageGalleryUtils;
import org.hippoecm.frontend.plugins.jquery.upload.single.BinaryContentEventLogger;
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
public class ImageCropEditorDialog extends Dialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(ImageCropEditorDialog.class);

    private static final IValueMap DIALOG_SIZE = new ValueMap("width=855,height=565").makeImmutable();
    private static final String DIALOG_TITLE = "edit-image-dialog-title";

    private static final int MAX_PREVIEW_WIDTH = 200;
    private static final int MAX_PREVIEW_HEIGHT = 300;

    private static final String WORKFLOW_CATEGORY = "cms";
    private static final String INTERACTION_TYPE_IMAGE = "image";
    private static final String ACTION_CROP = "crop";

    @SuppressWarnings("unused")
    private String region;
    private Dimension originalImageDimension;
    private Dimension configuredDimension;
    private Dimension thumbnailDimension;
    private float compressionQuality;

    private final IPluginConfig config;
    private final IPluginContext context;
    private final GalleryProcessor galleryProcessor;
    private final ImageCropSettings cropSettings;

    /**
     * A dialog to crop an image variant.
     *
     * @param variantImageNodeModel node where the variant to be cropped is stored.
     * @param galleryProcessor with configuration for scaling parameters and dimensions.
     * @param config Config of the instantiating Plugin.
     * @param context Context of the instantiating Plugin.
     */
    public ImageCropEditorDialog(final IModel<Node> variantImageNodeModel, final GalleryProcessor galleryProcessor,
                                 final IPluginConfig config, final IPluginContext context) {
        super(variantImageNodeModel);

        this.config = config;
        this.context = context;
        this.galleryProcessor = galleryProcessor;

        setSize(DIALOG_SIZE);
        setTitleKey(DIALOG_TITLE);

        final HiddenField<String> regionField = new HiddenField<>("region", new PropertyModel<>(this, "region"));
        regionField.setOutputMarkupId(true);
        add(regionField);

        final Node variantImageNode = variantImageNodeModel.getObject();
        Component originalImage, imgPreview;
        try {
            final Node originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(variantImageNode);
            originalImageDimension = ImageGalleryUtils.getDimension(originalImageNode);

            final JcrNodeModel originalNodeModel = new JcrNodeModel(originalImageNode);
            originalImage = new JcrImage("image", new JcrResourceStream(originalNodeModel));
            imgPreview = new JcrImage("imagepreview", new JcrResourceStream(originalNodeModel));

        } catch (RepositoryException e) {
            log.error("Cannot retrieve original image", e);
            error(e);
            originalImage = new EmptyPanel("image");
            imgPreview = new EmptyPanel("imagepreview");
        }

        final WebMarkupContainer imagePreviewContainer = new WebMarkupContainer("previewcontainer");
        imagePreviewContainer.setOutputMarkupId(true);
        try {
            configuredDimension = galleryProcessor.getDesiredResourceDimension(variantImageNode);
            thumbnailDimension = ImageUtils.normalizeDimension(originalImageDimension, configuredDimension);

            final double previewCropFactor = determinePreviewScalingFactor(thumbnailDimension.getWidth(), thumbnailDimension.getHeight());
            final double previewWidth = Math.floor(previewCropFactor * thumbnailDimension.getWidth());
            final double previewHeight = Math.floor(previewCropFactor * thumbnailDimension.getHeight());

            imagePreviewContainer.add(StyleAttribute.append("width:" + previewWidth + "px"));
            imagePreviewContainer.add(StyleAttribute.append("height:" + previewHeight + "px"));

        } catch (RepositoryException | GalleryException e) {
            log.error("Cannot retrieve thumbnail dimensions", e);
            error(e);
        }

        boolean isUpscalingEnabled = true;
        try {
            isUpscalingEnabled = galleryProcessor.isUpscalingEnabled(variantImageNode);
        } catch (GalleryException | RepositoryException e) {
            log.error("Cannot retrieve upscaling configuration option", e);
            error(e);
        }

        Label thumbnailSize = new Label("thumbnail-size", new StringResourceModel("thumbnail-size", this));
        thumbnailSize.setOutputMarkupId(true);
        add(thumbnailSize);
        //
        cropSettings = new ImageCropSettings(regionField.getMarkupId(),
                imagePreviewContainer.getMarkupId(),
                originalImageDimension,
                configuredDimension,
                thumbnailDimension,
                isUpscalingEnabled,
                true,
                thumbnailSize.getMarkupId(true));

        final ImageCropBehavior imageCropBehavior = new ImageCropBehavior(cropSettings);
        final IModel<Boolean> fitViewModel = new PropertyModel<>(this.cropSettings, "fitView");
        final AjaxCheckBox fitViewCheckbox = new AjaxCheckBox("fit-view", fitViewModel) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                executeFitInView(target, imageCropBehavior);
            }
        };
        fitViewCheckbox.setOutputMarkupId(true);
        add(fitViewCheckbox);

        originalImage.add(imageCropBehavior);
        originalImage.setOutputMarkupId(true);

        add(originalImage);
        imgPreview.add(StyleAttribute.append("position:absolute"));
        imagePreviewContainer.add(imgPreview);
        imagePreviewContainer.setVisible(cropSettings.isPreviewVisible());
        add(imagePreviewContainer);

        add(new Label("preview-description", cropSettings.isPreviewVisible() ?
                new StringResourceModel("preview-description-enabled", this) :
                new StringResourceModel("preview-description-disabled", this))
        );

        compressionQuality = 1.0f;
        try {
            compressionQuality = galleryProcessor.getScalingParametersMap().get(variantImageNode.getName()).getCompressionQuality();
        } catch (RepositoryException e) {
            log.info("Cannot retrieve compression quality.", e);
        }

    }

    /**
     * The {@link IPluginConfig} of the {@link ImageCropPlugin}.
     */
    protected IPluginConfig getPluginConfig() {
        return config;
    }

    /**
     * The {@link IPluginContext} of the {@link ImageCropPlugin}.
     */
    protected IPluginContext getPluginContext() {
        return context;
    }

    /**
     * Execute the fitInView function on the client-side widget instance
     */
    private void executeFitInView(final AjaxRequestTarget target, final ImageCropBehavior cropBehavior) {
        final String script = "fitInView(" + this.cropSettings.isFitView() + ")";
        target.appendJavaScript(cropBehavior.execWidgetFunction(script));
    }

    /**
     * Determine the scaling factor of the preview image, so that it fits within the max boundaries of
     * the preview container (e.g. {@code #MAX_PREVIEW_WIDTH} by {@code #MAX_PREVIEW_HEIGHT}).
     *
     * @param previewWidth width of preview image
     * @param previewHeight height of preview image
     * @return the scaling factor of the preview image
     */
    private double determinePreviewScalingFactor(final double previewWidth, final double previewHeight) {
        return determineScalingFactor(previewWidth, previewHeight, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
    }

    /**
     * Determine the scaling factor of the preview image, so that it fits within the max boundaries of
     * the preview container (e.g. {@code #MAX_PREVIEW_WIDTH} by {@code #MAX_PREVIEW_HEIGHT}).
     * @param width width of image
     * @param height height of image
     * @param maxWidth max width of image
     * @param maxHeight max height of image
     * @return the scaling factor of the preview image
     */
    private double determineScalingFactor(final double width, final double height, final double maxWidth, final double maxHeight) {

        final double widthBasedScaling;
        if (width > maxWidth) {
            widthBasedScaling = maxWidth / width;
        } else {
            widthBasedScaling = 1D;
        }

        final double heightBasedScaling;

        if (height > maxHeight) {
            heightBasedScaling = maxHeight / height;
        } else {
            heightBasedScaling = 1D;
        }

        if (heightBasedScaling < widthBasedScaling) {
            return heightBasedScaling;
        } else {
            return widthBasedScaling;
        }
    }

    @Override
    protected void onOk() {
        JSONObject jsonObject = JSONObject.fromObject(region);

        final Rectangle cropArea = new Rectangle(jsonObject.getInt("left"), jsonObject.getInt("top"),
                jsonObject.getInt("width"), jsonObject.getInt("height"));

        try {
            Node originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(getModelObject());
            String mimeType = originalImageNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            ImageReader reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }
            ImageWriter writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }

            final Binary binary = originalImageNode.getProperty(JcrConstants.JCR_DATA).getBinary();
            final MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(binary.getStream());
            reader.setInput(imageInputStream);

            final BufferedImage original = reader.read(0);
            final Dimension variantDimension = galleryProcessor.getDesiredResourceDimension(getModelObject());
            final Dimension dimension = ImageUtils.normalizeDimension(cropArea.getSize(), variantDimension);
            final BufferedImage variantImage = ImageUtils.scaleImage(original, cropArea, dimension,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC, ImageUtils.isCropHighQuality(cropArea, reader));
            final ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, variantImage, compressionQuality);

            //CMS7-8544 Keep the scaling of the image when cropping, to avoid a resulting image with bigger size than the original
            InputStream stored = new ByteArrayInputStream(bytes.toByteArray());
            final ScalingParameters parameters = galleryProcessor.getScalingParametersMap().get(getModelObject().getName());
            if (parameters != null) {
                try {
                    final ScaleImageOperation scaleOperation = new ScaleImageOperation(parameters);
                    scaleOperation.execute(stored, mimeType);
                    stored = scaleOperation.getScaledData();
                } catch (GalleryException e) {
                    log.warn("Scaling failed, using original image instead", e);
                }
            } else {
                log.debug("No scaling parameters specified for {}, using original image", galleryProcessor.getScalingParametersMap().get(getModelObject().getName()));
            }

            final Node cropped = getModelObject();
            cropped.setProperty(JcrConstants.JCR_DATA, ResourceHelper.getValueFactory(cropped).createBinary(stored));
            cropped.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
            cropped.setProperty(HippoGalleryNodeType.IMAGE_WIDTH, dimension.getWidth());
            cropped.setProperty(HippoGalleryNodeType.IMAGE_HEIGHT, dimension.getHeight());
            cropped.getSession().save();

            BinaryContentEventLogger.fireBinaryChangedEvent(cropped, WORKFLOW_CATEGORY, INTERACTION_TYPE_IMAGE, ACTION_CROP);
        } catch (GalleryException | IOException | RepositoryException ex) {
            log.error("Unable to crop image", ex);
            error(ex);
        }
    }

    @Override
    protected boolean isFullscreenEnabled() {
        return true;

    }
}
