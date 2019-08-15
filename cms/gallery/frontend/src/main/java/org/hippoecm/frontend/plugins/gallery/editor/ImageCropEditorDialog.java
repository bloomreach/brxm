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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropBehavior;
import org.hippoecm.frontend.plugins.gallery.editor.crop.ImageCropSettings;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.util.ImageGalleryUtils;
import org.hippoecm.frontend.plugins.jquery.upload.single.BinaryContentEventLogger;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.resource.JcrResourceStream;
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

    private static final String WORKFLOW_CATEGORY = "cms";
    private static final String INTERACTION_TYPE_IMAGE = "image";
    private static final String ACTION_CROP = "crop";

    @SuppressWarnings("unused")
    private String region;
    private Dimension originalImageDimension;
    private Dimension configuredDimension;
    private Dimension previewDimension;

    private final IPluginConfig config;
    private final IPluginContext context;
    private final GalleryProcessor galleryProcessor;
    private final ImageCropSettings cropSettings;
    private JcrNodeModel originalNodeModel;

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

        final Node variantImageNode = getModelObject();
        final Node originalImageNode;
        try {
            originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(variantImageNode);
            originalNodeModel = new JcrNodeModel(originalImageNode);
            originalImageDimension = ImageGalleryUtils.getDimension(originalImageNode);
        } catch (RepositoryException e) {
            log.error("Cannot retrieve original image", e);
            error(e);
            originalNodeModel = null;
        }

        final Component originalImage = originalNodeModel != null
                ? new JcrImage("image", new JcrResourceStream(originalNodeModel))
                : new EmptyPanel("image");
        originalImage.setOutputMarkupId(true);
        add(originalImage);

        final ImagePreviewComponent previewImage = new ImagePreviewComponent("previewcontainer", originalNodeModel);
        try {
            configuredDimension = galleryProcessor.getDesiredResourceDimension(variantImageNode);
            previewDimension = ImageUtils.normalizeDimension(originalImageDimension, configuredDimension);
            previewImage.setDimension(previewDimension);
        } catch (RepositoryException | GalleryException e) {
            log.error("Cannot retrieve preview dimensions", e);
            error(e);
        }

        final Label thumbnailSize = new Label("thumbnail-size", new StringResourceModel("thumbnail-size", this));
        thumbnailSize.setOutputMarkupId(true);
        add(thumbnailSize);

        final ScalingParameters parameters = galleryProcessor.getScalingParameters(variantImageNode);
        cropSettings = new ImageCropSettings(regionField.getMarkupId(),
                previewImage.getMarkupId(),
                originalImageDimension,
                configuredDimension,
                previewDimension,
                parameters != null && parameters.isUpscaling(),
                true,
                thumbnailSize.getMarkupId(true));

        final ImageCropBehavior imageCropBehavior = new ImageCropBehavior(cropSettings);
        final IModel<Boolean> fitViewModel = new PropertyModel<>(cropSettings, "fitView");
        final AjaxCheckBox fitViewCheckbox = new AjaxCheckBox("fit-view", fitViewModel) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                executeFitInView(target, imageCropBehavior);
            }
        };
        fitViewCheckbox.setOutputMarkupId(true);
        add(fitViewCheckbox);

        originalImage.add(imageCropBehavior);
        previewImage.setVisible(cropSettings.isPreviewVisible());
        add(previewImage);

        add(new Label("preview-description", cropSettings.isPreviewVisible() ?
                new StringResourceModel("preview-description-enabled", this) :
                new StringResourceModel("preview-description-disabled", this))
        );
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
        final String script = "fitInView(" + cropSettings.isFitView() + ")";
        target.appendJavaScript(cropBehavior.execWidgetFunction(script));
    }

    @Override
    protected void onOk() {
        try {
            final Node variantNode = getModelObject();
            final Node originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(variantNode);

            final String mimeType = originalImageNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            final ImageReader reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }

            final Binary binary = originalImageNode.getProperty(JcrConstants.JCR_DATA).getBinary();
            final MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(binary.getStream());
            reader.setInput(imageInputStream);

            final ImageWriter writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }

            final Rectangle cropArea = getRectangleFromJSON(region);
            final Dimension variantDimension = galleryProcessor.getDesiredResourceDimension(variantNode);
            final Dimension targetDimension  = ImageUtils.normalizeDimension(cropArea.getSize(), variantDimension);
            final ScalingParameters parameters = galleryProcessor.getScalingParameters(variantNode);
            final float compressionQuality = parameters != null
                    ? parameters.getCompressionQuality()
                    : 1.0f;

            final BufferedImage originalImage = reader.read(0);
            BufferedImage variantImage = ImageUtils.cropImage(originalImage, cropArea);

            if (parameters == null) {
                log.debug("No scaling parameters specified for {}, using original image", variantNode.getName());
            } else {
                // CMS7-8544 Keep the scaling of the image when cropping, to avoid a resulting image with bigger size
                // than the original
                variantImage = ImageUtils.scaleImage(variantImage, targetDimension.width, targetDimension.height,
                        parameters.getStrategy());
            }
            final ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, variantImage, compressionQuality);
            saveImageNode(variantNode, new ByteArrayInputStream(bytes.toByteArray()), targetDimension);

        } catch (GalleryException | IOException | RepositoryException ex) {
            log.error("Unable to crop image", ex);
            error(ex);
        }
    }

    @Override
    protected boolean isFullscreenEnabled() {
        return true;
    }

    @Override
    protected void onDetach() {
        if (originalNodeModel != null) {
            originalNodeModel.detach();
        }
        super.onDetach();
    }

    private static void saveImageNode(final Node node, final InputStream inputStream, final Dimension dimension)
            throws RepositoryException {

        ImageGalleryUtils.saveImageNode(node, inputStream, dimension.width, dimension.height);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
        node.getSession().save();

        BinaryContentEventLogger.fireBinaryChangedEvent(node, WORKFLOW_CATEGORY, INTERACTION_TYPE_IMAGE, ACTION_CROP);
    }

    private static Rectangle getRectangleFromJSON(final String jsonString) {
        final JSONObject json = JSONObject.fromObject(jsonString);
        return new Rectangle(json.getInt("left"), json.getInt("top"), json.getInt("width"), json.getInt("height"));
    }

    private static class ImagePreviewComponent extends WebMarkupContainer {

        private static final int MAX_PREVIEW_WIDTH = 200;
        private static final int MAX_PREVIEW_HEIGHT = 300;

        ImagePreviewComponent(final String id, final JcrNodeModel imageNodeModel) {
            super(id);

            setOutputMarkupId(true);

            if (imageNodeModel == null) {
                add(new EmptyPanel("imagepreview"));
            } else {
                final Component previewImage = new JcrImage("imagepreview", new JcrResourceStream(imageNodeModel));
                previewImage.add(StyleAttribute.append("position:absolute"));
                add(previewImage);
            }
        }

        void setDimension(final Dimension dimension) {
            final double width = dimension.getWidth();
            final double height = dimension.getHeight();
            final double previewCropFactor = ImagePreviewComponent.determinePreviewScalingFactor(width, height);
            final double previewWidth = Math.floor(previewCropFactor * width);
            final double previewHeight = Math.floor(previewCropFactor * height);

            add(StyleAttribute.append("width:" + previewWidth + "px"));
            add(StyleAttribute.append("height:" + previewHeight + "px"));
        }

        /**
         * Determine the scaling factor of the preview image, so that it fits within the max boundaries of
         * the preview container (e.g. {@code #MAX_PREVIEW_WIDTH} by {@code #MAX_PREVIEW_HEIGHT}).
         *
         * @param previewWidth width of preview image
         * @param previewHeight height of preview image
         * @return the scaling factor of the preview image
         */
        private static double determinePreviewScalingFactor(final double previewWidth, final double previewHeight) {
            return ImageUtils.determineScalingFactor(previewWidth, previewHeight, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
        }
    }
}
