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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
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
    private static final String CROP_BUTTON_ID = "crop-button";

    public ImageCropPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(isEditMode() ? createCropButton() : createHiddenButton());
    }

    private boolean isEditMode() {
        final IEditor.Mode mode = IEditor.Mode.fromString(getPluginConfig().getString("mode"), IEditor.Mode.EDIT);
        return mode == IEditor.Mode.EDIT;
    }

    private Component createCropButton() {
        final Node imageNode = getModelObject();
        // Check if this is the original image
        try {
            if (ImageGalleryUtils.isOriginalImage(imageNode)) {
                return createHiddenButton();
            }
        } catch (RepositoryException e) {
            error(e);
            log.error("Cannot retrieve original image node", e);
            return createErrorButton();
        }

        final IPluginContext context = getPluginContext();
        final IPluginConfig config = getPluginConfig();
        final GalleryProcessor processor = DefaultGalleryProcessor.getGalleryProcessor(context, config);
        try {
            final Dimension variantDimension = processor.getDesiredResourceDimension(imageNode);
            final Node originalImageNode = ImageGalleryUtils.getOriginalGalleryNode(imageNode);
            final Dimension originalDimension = ImageGalleryUtils.getDimension(originalImageNode);

            final ScalingParameters params = processor.getScalingParameters(imageNode);
            if (params == null || !params.isUpscaling()) {
                if (variantDimension.getWidth() > originalDimension.getWidth()) {
                    return createDisabledButton("crop-button-tip-inactive-width");
                }

                if (variantDimension.getHeight() > originalDimension.getHeight()) {
                    return createDisabledButton("crop-button-tip-inactive-height");
                }
            }
        } catch (RepositoryException | GalleryException | NullPointerException e) {
            error(e);
            log.error("Cannot retrieve dimensions of original or variant image", e);
            return createErrorButton();
        }

        final Component cropButton = createButton("crop-button-tip", "crop-button active");
        cropButton.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                final String serviceName = IDialogService.class.getName();
                final IDialogService dialogService = context.getService(serviceName, IDialogService.class);
                dialogService.show(new ImageCropEditorDialog(getModel(), processor, config, context));
            }
        });

        return cropButton;
    }

    private Component createErrorButton() {
        return createDisabledButton("crop-button-tip-inactive-error");
    }

    private Component createDisabledButton(final String titleKey) {
        return createButton(titleKey, "crop-button inactive");
    }

    private Component createButton(final String titleKey, final String cssClass) {
        final Label label = new Label(CROP_BUTTON_ID, new StringResourceModel("crop-button-label", this));
        label.add(TitleAttribute.append(new StringResourceModel(titleKey, this)));
        label.add(ClassAttribute.append(cssClass));
        return label;
    }

    private Component createHiddenButton() {
        return new EmptyPanel(CROP_BUTTON_ID);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CROP_SKIN));
    }
}
