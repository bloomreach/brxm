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

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;


import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ImageRegeneratePlugin extends RenderPlugin {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ImageRegeneratePlugin.java 27169 2011-03-01 14:25:35Z mchatzidakis $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageRegeneratePlugin.class);

    private GalleryProcessor galleryProcessor;

    public ImageRegeneratePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(CSSPackageResource.getHeaderContribution(ImageCropPlugin.class, "regenerate-plugin.css"));

        String mode = config.getString("mode", "edit");
        galleryProcessor = context.getService(getPluginConfig().getString("gallery.processor.id", "gallery.processor.service"), GalleryProcessor.class);

        boolean isOriginal = true;
        boolean areExceptionsThrown = false;
        boolean isThumbnailModified = false;

        try{
            isOriginal = HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(((Node) getModel().getObject()).getName());
        } catch(RepositoryException e){
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        try{
            Node thumbnailImageNode = (Node) getModelObject();
            Node originalImageNode = thumbnailImageNode.getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
            isThumbnailModified = originalImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate().equals(thumbnailImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate());
        } catch(RepositoryException e){
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        Label regenerateButton = new Label("regenerate-button", new StringResourceModel("regenerate-button-label", this, null));
        regenerateButton.setVisible("edit".equals(mode));

        if("edit".equals(mode)){
            if(!isOriginal && !areExceptionsThrown && isThumbnailModified) {
                regenerateButton.add(new AjaxEventBehavior("onclick") {
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        regenerateThumbnail();
                    }
                });
            }

            regenerateButton.add(new AttributeAppender("class", new Model<String>(
                (isOriginal || areExceptionsThrown || !isThumbnailModified) ? "regenerate-button inactive" : "regenerate-button active"
            ), " "));

            String buttonTipProperty =
                isOriginal ? "regenerate-button-tip-inactive-original" :
                areExceptionsThrown ? "regenerate-button-tip-inactive-error" :
                !isThumbnailModified ? "regenerate-button-tip-inactive-not-modified" :
                "regenerate-button-tip";

            regenerateButton.add(new AttributeAppender("title", new Model<String>(new StringResourceModel(buttonTipProperty, this, null).getString()), ""));
        }

        add(regenerateButton);
    }

    private void regenerateThumbnail() {
        try {
            Node thumbnailImageNode = (Node) getModelObject();
            Node imageSetNode = thumbnailImageNode.getParent();
            Node originalImageNode = imageSetNode.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
            String mimeType = originalImageNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            String filename = imageSetNode.getProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME).getString();

            galleryProcessor.initGalleryResource(
                thumbnailImageNode,
                originalImageNode.getProperty(JcrConstants.JCR_DATA).getStream(),
                mimeType,
                filename,
                originalImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate()
            );

        } catch (GalleryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        } catch (RepositoryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        }

    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }
}
