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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageCropPlugin extends RenderPlugin {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ImageCropPlugin.java 27169 2011-03-01 14:25:35Z mchatzidakis $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageCropPlugin.class);


    public ImageCropPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(CSSPackageResource.getHeaderContribution(ImageCropPlugin.class, "crop-plugin.css"));

        String mode = config.getString("mode", "edit");
        final IModel<Node> jcrImageNodeModel = getModel();
        GalleryProcessor _processor = context.getService(getPluginConfig().getString("gallery.processor.id", "gallery.processor.service"), GalleryProcessor.class);
        final GalleryProcessor processor = _processor == null ? new DefaultGalleryProcessor() : _processor;

        boolean isOriginal = true;
        boolean isOriginalImageWidthSmallerThanThumbWidth = false;
        boolean isOriginalImageHeightSmallerThanThumbHeight = false;
        boolean areExceptionsThrown = false;

        //Check if this is the original image
        try {
            isOriginal = HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(jcrImageNodeModel.getObject().getName());
        } catch (RepositoryException e) {
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        //Get dimensions of this thumbnail variant
        try {
            Dimension thumbnailDimension = processor.getDesiredResourceDimension((Node) jcrImageNodeModel.getObject());
            Node originalImageNode = ((Node) getModelObject()).getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
            Dimension originalImageDimension = new Dimension(
                    (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getLong(),
                    (int) originalImageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getLong());

            isOriginalImageWidthSmallerThanThumbWidth = thumbnailDimension.getWidth() > originalImageDimension.getWidth();
            isOriginalImageHeightSmallerThanThumbHeight = thumbnailDimension.getHeight() > originalImageDimension.getHeight();

        } catch (RepositoryException e) {
            error(e);
            log.error("Cannot retrieve dimensions of original or thumbnail image", e);
            areExceptionsThrown = true;
        } catch (GalleryException e) {
            error(e);
            log.error("Cannot retrieve dimensions of original or thumbnail image", e);
            areExceptionsThrown = true;
        }

        Label cropButton = new Label("crop-button", new StringResourceModel("crop-button-label", this, null));
        cropButton.setVisible("edit".equals(mode) && !isOriginal);

        if ("edit".equals(mode)) {
            if (!isOriginal && !areExceptionsThrown && !isOriginalImageWidthSmallerThanThumbWidth && !isOriginalImageHeightSmallerThanThumbHeight) {
                cropButton.add(new AjaxEventBehavior("onclick") {
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                        dialogService.show(new ImageCropEditorDialog(jcrImageNodeModel, processor));
                    }
                });
            }

            cropButton.add(new AttributeAppender("class", new Model<String>(
                    (isOriginal || areExceptionsThrown || isOriginalImageWidthSmallerThanThumbWidth || isOriginalImageHeightSmallerThanThumbHeight) ? "crop-button inactive" : "crop-button active"
            ), " "));
            String buttonTipProperty =
                    areExceptionsThrown ? "crop-button-tip-inactive-error" :
                            isOriginalImageWidthSmallerThanThumbWidth ? "crop-button-tip-inactive-width" :
                                    isOriginalImageHeightSmallerThanThumbHeight ? "crop-button-tip-inactive-height" :
                                            "crop-button-tip";

            cropButton.add(new AttributeAppender("title", new Model<String>(new StringResourceModel(buttonTipProperty, this, null).getString()), ""));
        }

        add(cropButton);
    }
}
