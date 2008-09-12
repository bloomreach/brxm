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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.resource.StringResourceStream;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.upload.UploadDialog;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;

public class GalleryShortcutPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public GalleryShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
                private static final long serialVersionUID = 1L;

                @Override
                    public void onClick(AjaxRequestTarget target) {
                    IDialogService dialogService = getDialogService();
                    dialogService.show(new UploadDialog(GalleryShortcutPlugin.this,
                                                        GalleryShortcutPlugin.this.getPluginContext(),
                                                        GalleryShortcutPlugin.this.getPluginConfig(),
                                                        dialogService));
                }
            };
        add(link);

        Label label = new Label("label");
        label.setModel(new StringResourceModel(config.getString("gallery.text"), this, null));
        link.add(label);

        String path = config.getString("gallery.path");
        if(path != null) {
            try {
                while(path.startsWith("/"))
                    path = path.substring(1);
                setModel(new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path)));
                // HREPTWO-1218 getModel returns null, which causes problems for the WizardDialog
            } catch(PathNotFoundException ex) {
                Gallery.log.warn("No image gallery present");
                path = null; // force adding empty panel
            } catch(RepositoryException ex) {
                Gallery.log.warn("Error while accessing image gallery");
                path = null; // force adding empty panel
            }
        }
        if(path == null) {
            link.setVisible(false);
            label.setVisible(false);
        }
    }
}
