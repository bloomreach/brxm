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
package org.hippoecm.frontend.plugins.cms.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPerspective extends Perspective implements IBrowseService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(BrowserPerspective.class);

    private static final long serialVersionUID = 1L;

    private ModelService modelService;
    private boolean isGallery = false;
    private Component listWrapper;
    private Component galleryWrapper;
    private String listingTitle = "documents"; 

    public BrowserPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(RenderPlugin.MODEL_ID) != null) {
            modelService = new ModelService(config.getString(RenderPlugin.MODEL_ID), new JcrNodeModel("/"));
            modelService.init(context);
        } else {
            log.error("no model service specified");
        }

        if (config.getString(IBrowseService.BROWSER_ID) != null) {
            context.registerService(this, config.getString(IBrowseService.BROWSER_ID));
        }

        add(listWrapper = new Wrapper("list.wrapper"));
        add(galleryWrapper = new Wrapper("gallery.wrapper").setEnabled(false));

        //add(new Label("listing.title",new PropertyModel(this,"listingTitle")));
      
        add(new Label("listing.title",new PropertyModel(this,"listingTitle")){
            private static final long serialVersionUID = 1L;
            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("class", listingTitle);
            }
        }); 
      
        // model didn't exist for super constructor, so set it explicitly
        updateModel(modelService.getModel());
    }

    public void browse(IModel model) {
        modelService.setModel(model);
        focus(null);
    }

    @Override
    public void onModelChanged() {
        IModel model = getModel();
        if (model != null && model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
                if (node.isNodeType("hippostd:gallery")) {
                    if (!isGallery) {
                        isGallery = true;
                        galleryWrapper.setEnabled(true);
                        listingTitle = "gallery";
                        listWrapper.setEnabled(false);
                        redraw();
                    }
                } else {
                    if (isGallery) {
                        listWrapper.setEnabled(true);
                        listingTitle = "documents";
                        galleryWrapper.setEnabled(false);
                        isGallery = false;
                        redraw();
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            isGallery = false;
            listWrapper.setEnabled(true);
            galleryWrapper.setEnabled(false);
            redraw();
        }
    }

    private class Wrapper extends WebMarkupContainer {
        private static final long serialVersionUID = 1L;

        Wrapper(String id) {
            super(id);
        }

        @Override
        public boolean isTransparentResolver() {
            return true;
        }

        @Override
        public void onComponentTag(final ComponentTag tag) {
            super.onComponentTag(tag);

            if (isEnabled()) {
                tag.put("style", "display: block;");
            } else {
                tag.put("style", "display: none;");
            }
        }
    }

}
