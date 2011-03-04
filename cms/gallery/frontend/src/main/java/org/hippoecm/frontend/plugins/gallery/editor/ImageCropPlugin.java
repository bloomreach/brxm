/*
 *  Copyright 2008 Hippo.
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


import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageCropPlugin extends RenderPlugin {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ImageCropPlugin.java 27169 2011-03-01 14:25:35Z mchatzidakis $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageCropPlugin.class);


    public ImageCropPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        String mode = config.getString("mode", "edit");
        final IModel<Node> jcrImageNodeModel = getModel();

        try{
            boolean isOriginal = "hippogallery:original".equals(jcrImageNodeModel.getObject().getName());

            final GalleryProcessor processor = context.getService(getPluginConfig().getString("gallery.processor.id", "gallery.processor.service"), GalleryProcessor.class);

            //The edit image button
            AjaxLink<String> cropLink = new AjaxLink<String>("crop-link") {
                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                    dialogService.show(new ImageCropEditorDialog(jcrImageNodeModel, (processor == null ? new DefaultGalleryProcessor() : processor)));
                }
            };

            cropLink.setVisible("edit".equals(mode) && !isOriginal);
            //for now, hide this button
            cropLink.setVisible(false);
            add(cropLink);

        } catch(RepositoryException ex){
            error(ex);
            log.error(ex.getMessage());
        }
    }
}
