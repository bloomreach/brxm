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
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
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

        try{
            isOriginal = "hippogallery:original".equals(((Node)getModel().getObject()).getName());
        } catch(RepositoryException e){
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        Label regenerateButton = new Label("regenerate-button", new StringResourceModel("regenerate-button-label", this, null));
        regenerateButton.add(new AjaxEventBehavior("onclick") {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                regenerateThumbnail();
            }
        });

        regenerateButton.setVisible("edit".equals(mode) && !isOriginal && !areExceptionsThrown);
        add(regenerateButton);
    }

    private void regenerateThumbnail() {
        try {
            Node modelObject = (Node) getModelObject();
            Node originalImageNode = modelObject.getParent().getNode("hippogallery:original");
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
            Object hints = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
            boolean highQuality = true;

            Dimension dimension = galleryProcessor.getDesiredResourceDimension((Node)getModelObject());
            int targetWidth = (int)dimension.getWidth();
            int targetHeight = (int)dimension.getHeight();

            BufferedImage thumbnail = ImageUtils.scaleImage(original, targetWidth, targetHeight, hints, highQuality);
            ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, thumbnail);

            modelObject.getProperty(JcrConstants.JCR_DATA).setValue(new ByteArrayInputStream(bytes.toByteArray()));
            modelObject.setProperty(JcrConstants.JCR_LASTMODIFIED, originalImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate());

            //The following code can originally be found in org.hippoecm.frontend.plugins.gallery.imageutil.ScaleImageOperation
            double resizeRatio = calculateResizeRatio(original.getWidth(), original.getHeight(), targetWidth, targetHeight);
            int actualWidth = (int)Math.max(original.getWidth() * resizeRatio, 1);
            int actualHeight = (int)Math.max(original.getHeight() * resizeRatio, 1);
            modelObject.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).setValue(actualWidth);
            modelObject.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).setValue(actualHeight);

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

    //The following code can originally be found in org.hippoecm.frontend.plugins.gallery.imageutil.ScaleImageOperation
    private double calculateResizeRatio(double originalWidth, double originalHeight, int targetWidth, int targetHeight) {
        double widthRatio = 1;
        double heightRatio = 1;

        if (targetWidth >= 1) {
            widthRatio = targetWidth / originalWidth;
        }
        if (targetHeight >= 1) {
            heightRatio = targetHeight / originalHeight;
        }

        // If the image has to be scaled down we should return the largest negative ratio.
        // If the image has to be scaled up, and we should take the smallest positive ratio.
        return Math.min(widthRatio, heightRatio);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }
}
