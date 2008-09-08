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
package org.hippoecm.frontend.plugins.gallery.upload;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.Gallery;
import org.hippoecm.frontend.plugins.gallery.GalleryShortcutPlugin;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.session.UserSession;

public class UploadDialog extends WebPage implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String title;
    private UploadWizard wizard;
    private IServiceReference<IJcrService> jcrServiceRef;
    private String workflowCategory;
    private String exception = "";

    public UploadDialog(GalleryShortcutPlugin plugin, IPluginContext context, IPluginConfig config,
            IDialogService dialogWindow) {
        try {
            String path = config.getString("gallery.path");
            if (path != null) {
                while (path.startsWith("/"))
                    path = path.substring(1);
                setModel(new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path)));
            }
        } catch (PathNotFoundException ex) {
            // cannot occur anymore because GalleryShortcutPlugin already checked this, however
            // because of HREPTWO-1218 we cannot use the model of GalleryShortcutPlugin.
        } catch (RepositoryException ex) {
            Gallery.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }

        title = config.getString("gallery.text");
        if(title == null) {
            title = "";
        }
        title = title.trim();
        
        Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
        jcrServiceRef = context.getReference(service);
        workflowCategory = config.getString("gallery.workflow");
        wizard = new UploadWizard("wizard", dialogWindow, this); 
        add(wizard);
    }

    public String getException() {
        return exception;
    }
    
    public void setException(String exception) {
        this.exception = exception;
    }

    public String getWorkflowCategory() {
        return workflowCategory;
    }
    
    public UploadWizard getWizard() {
        return wizard;
    }

    public IJcrService getJcrService() {
        return jcrServiceRef.getService();
    }

    @Override
    public void onDetach() {
        if (jcrServiceRef != null) {
            jcrServiceRef.detach();
        }
        super.onDetach();
    }

    public String getTitle() {
        return title;
    }

}
