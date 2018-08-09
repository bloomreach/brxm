/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageRegeneratePlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageRegeneratePlugin.class);

    private static final CssResourceReference SKIN = new CssResourceReference(ImageCropPlugin.class, "regenerate-plugin.css");

    private GalleryProcessor galleryProcessor;
    private boolean isOriginal;
    private boolean areExceptionsThrown;
    private IModel<Boolean> isModelModified;

    public ImageRegeneratePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.EDIT);
        galleryProcessor = DefaultGalleryProcessor.getGalleryProcessor(context, getPluginConfig());

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
            private static final long serialVersionUID = 1L;
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


        Label regenerateButton = new Label("regenerate-button", new StringResourceModel("regenerate-button-label", this)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isEnabled() {
                return !isOriginal && !areExceptionsThrown && isModelModified.getObject();
            }
        };
        regenerateButton.setVisible(mode == IEditor.Mode.EDIT && !isOriginal);

        if (mode == IEditor.Mode.EDIT) {

            regenerateButton.add(new AjaxEventBehavior("click") {
                private static final long serialVersionUID = 1L;
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    regenerateThumbnail();
                }
            });


            regenerateButton.add(CssClass.append(new LoadableDetachableModel<String>(){
                private static final long serialVersionUID = 1L;
                @Override
                protected String load(){
                   return (isOriginal || areExceptionsThrown || !isModelModified.getObject()) ? "regenerate-button inactive" : "regenerate-button active";
                }
            }));


            regenerateButton.add(TitleAttribute.append(new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;
                @Override
                protected String load() {
                    String buttonTipProperty =
                            areExceptionsThrown ? "regenerate-button-tip-inactive-error" :
                                    !isModelModified.getObject() ? "regenerate-button-tip-inactive-not-modified" :
                                            "regenerate-button-tip";

                    return new StringResourceModel(buttonTipProperty, ImageRegeneratePlugin.this).getString();
                }
            }));

        }

        add(regenerateButton);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(SKIN));
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

        } catch (GalleryException | RepositoryException ex) {
            log.error("Unable to create thumbnail image", ex);
            error(ex);
        }

    }

    @Override
    protected void onDetach() {
        isModelModified.detach();
        super.onDetach();
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }
}
