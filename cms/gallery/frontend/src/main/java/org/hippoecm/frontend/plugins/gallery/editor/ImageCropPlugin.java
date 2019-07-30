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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.util.ImageGalleryUtils;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageCropPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(ImageCropPlugin.class);

    private static final CssResourceReference CROP_SKIN = new CssResourceReference(ImageCropPlugin.class, "crop-plugin.css");

    public ImageCropPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.EDIT);
        final GalleryProcessor processor = DefaultGalleryProcessor.getGalleryProcessor(context, getPluginConfig());
        final IModel<Node> imageNodeModel = getModel();
        final Node imageNode = imageNodeModel.getObject();

        boolean upscalingEnabled = false;
        boolean isOriginal = true;
        boolean isOriginalImageWidthSmallerThanVariantWidth = false;
        boolean isOriginalImageHeightSmallerThanVariantHeight = false;
        boolean areExceptionsThrown = false;

        // Check if this is the original image
        try {
            isOriginal = ImageGalleryUtils.isOriginalImage(imageNode);
        } catch (RepositoryException e) {
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        // Get dimensions of this thumbnail variant
        try {
            final Dimension variantDimension = processor.getDesiredResourceDimension(imageNode);
            final Node originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(imageNode);
            final Dimension originalImageDimension = ImageGalleryUtils.getDimension(originalImageNode);

            isOriginalImageWidthSmallerThanVariantWidth = variantDimension.getWidth() > originalImageDimension.getWidth();
            isOriginalImageHeightSmallerThanVariantHeight = variantDimension.getHeight() > originalImageDimension.getHeight();

            upscalingEnabled = processor.isUpscalingEnabled(imageNode);

        } catch (RepositoryException | GalleryException | NullPointerException e) {
            error(e);
            log.error("Cannot retrieve dimensions of original or variant image", e);
            areExceptionsThrown = true;
        }

        final Label cropButton = new Label("crop-button", new StringResourceModel("crop-button-label", this));
        cropButton.setVisible(mode == IEditor.Mode.EDIT && !isOriginal);


        final boolean isUpdateDisabled =
                isOriginal
                || areExceptionsThrown
                || (isOriginalImageWidthSmallerThanVariantWidth && !upscalingEnabled)
                || (isOriginalImageHeightSmallerThanVariantHeight && !upscalingEnabled);

        if (mode == IEditor.Mode.EDIT) {
            if (!isUpdateDisabled) {

                cropButton.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                        dialogService.show(new ImageCropEditorDialog(imageNodeModel, processor, config, context));
                    }
                });
            }

            cropButton.add(ClassAttribute.append(isUpdateDisabled
                    ? "crop-button inactive"
                    : "crop-button active"));

            final String buttonTipProperty;
            if (areExceptionsThrown) {
                buttonTipProperty = "crop-button-tip-inactive-error";
            } else if(isOriginalImageWidthSmallerThanVariantWidth && !upscalingEnabled) {
                buttonTipProperty = "crop-button-tip-inactive-width";
            } else if (isOriginalImageHeightSmallerThanVariantHeight && !upscalingEnabled) {
                buttonTipProperty = "crop-button-tip-inactive-height";
            } else {
                buttonTipProperty = "crop-button-tip";
            }

            cropButton.add(TitleAttribute.append(new StringResourceModel(buttonTipProperty, this)));
        }

        add(cropButton);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CROP_SKIN));
    }
}
