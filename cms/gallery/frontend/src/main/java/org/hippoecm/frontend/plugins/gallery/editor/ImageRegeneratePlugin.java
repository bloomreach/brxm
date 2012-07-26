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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageRegeneratePlugin extends RenderPlugin {


    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageRegeneratePlugin.class);

    private GalleryProcessor galleryProcessor;
    private boolean isOriginal;
    private boolean areExceptionsThrown;
    private IModel<Boolean> isModelModified;

    public ImageRegeneratePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(CSSPackageResource.getHeaderContribution(ImageCropPlugin.class, "regenerate-plugin.css"));

        String mode = config.getString("mode", "edit");
        galleryProcessor = context.getService(getPluginConfig().getString("gallery.processor.id", "gallery.processor.service"), GalleryProcessor.class);

        isOriginal = true;
        areExceptionsThrown = false;

        try{
            isOriginal = HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(((Node) getModel().getObject()).getName());
        } catch(RepositoryException e){
            error(e);
            log.error("Cannot retrieve name of original image node", e);
            areExceptionsThrown = true;
        }

        isModelModified = new LoadableDetachableModel<Boolean>(){
            @Override
            protected Boolean load() {
                 try{
                    Node thumbnailImageNode = (Node) getModelObject();
                    Node originalImageNode = thumbnailImageNode.getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
                    return ! originalImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate().equals(thumbnailImageNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate());
                } catch(RepositoryException e){
                    error(e);
                    log.error("Cannot retrieve name of original image node", e);
                    areExceptionsThrown = true;
                }
                return false;
            }
        };


        Label regenerateButton = new Label("regenerate-button", new StringResourceModel("regenerate-button-label", this, null)) {
            @Override
            public boolean isEnabled() {
                return !isOriginal && !areExceptionsThrown && isModelModified.getObject();
            }
        };
        regenerateButton.setVisible("edit".equals(mode) && !isOriginal);

        if("edit".equals(mode)){

            regenerateButton.add(new AjaxEventBehavior("onclick") {
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    regenerateThumbnail();
                }
            });


            regenerateButton.add(new AttributeAppender("class", new LoadableDetachableModel<String>(){
                @Override
                protected String load(){
                   return (isOriginal || areExceptionsThrown || !isModelModified.getObject()) ? "regenerate-button inactive" : "regenerate-button active";
                }
            }, " "));


            regenerateButton.add(new AttributeAppender("title", new LoadableDetachableModel<String>() {
                @Override
                protected String load() {
                    String buttonTipProperty =
                    areExceptionsThrown ? "regenerate-button-tip-inactive-error" :
                    !isModelModified.getObject() ? "regenerate-button-tip-inactive-not-modified" :
                    "regenerate-button-tip";

                    return new StringResourceModel(buttonTipProperty, ImageRegeneratePlugin.this, null).getString();
                }
            }, " "));

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
    protected void onDetach() {
        isModelModified.detach();
        super.onDetach();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }
}
