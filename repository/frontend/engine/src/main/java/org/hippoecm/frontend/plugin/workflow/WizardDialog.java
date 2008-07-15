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
package org.hippoecm.frontend.plugin.workflow;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowManager;

public class WizardDialog extends WebPage {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private JcrNodeModel model;
    private IServiceReference<IJcrService> jcrServiceRef;
    private WizardForm form;
    private String workflowCategory;
    protected AjaxLink submit;
    private IServiceReference<IDialogService> windowRef;
    private String exception = "";

    public WizardDialog(GalleryShortcutPlugin plugin, IPluginContext context, IPluginConfig config, IDialogService dialogWindow) {
        //super(context, dialogWindow);
        this.windowRef = context.getReference(dialogWindow);
        this.model = (JcrNodeModel) plugin.getModel();

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel); 

        IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
        jcrServiceRef = context.getReference(service);

        workflowCategory = config.getString("gallery.path");
        add(form = new WizardForm("form", model));

        submit = new AjaxLink("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    // do the action to be performed for user
                    ((UserSession) Session.get()).getJcrSession().refresh(true);
                    IJcrService jcrService = jcrServiceRef.getService();
                    if (jcrService != null) {
                        jcrService.flush(new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getRootNode()));
                    }

                    windowRef.getService().close();
                } catch (Exception e) {
                    String msg = e.getClass().getName() + ": " + e.getMessage();
                    Gallery.log.error(msg);
                    if (Gallery.log.isDebugEnabled()) {
                        Gallery.log.debug("Error from repository: ", e);
                    }
                    exception = msg;
                    target.addComponent(exceptionLabel);
                    e.printStackTrace();
                }
            }
        };
        add(submit);
    }

    @Override
    public void onDetach() {
        if(model != null) {
            model.detach();
        }
        if(jcrServiceRef != null) {
            jcrServiceRef.detach();
        }
        super.onDetach();
    }
    
    private class WizardForm extends Form {
        private static final long serialVersionUID = 1L;

        private final FileUploadField uploadField;

        public WizardForm(String name, JcrNodeModel model) {
            super(name, model);
            setMultiPart(true);
            setMaxSize(Bytes.megabytes(5));
            add(uploadField = new FileUploadField("input"));
        }

        @Override
        protected void onSubmit() {
            final FileUpload upload = uploadField.getFileUpload();
            if (upload != null) {
                try {
                    String filename = upload.getClientFileName();
                    String mimetype = upload.getContentType();
                    InputStream istream = upload.getInputStream();

                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    JcrNodeModel model = (JcrNodeModel) getModel();
                    if(model != null && model.getNode() != null) {
                        try {
                            Node galleryNode = model.getNode();
                            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowCategory, galleryNode);
                            Document document = workflow.createGalleryItem(filename);
                            Node node = (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                            Item item = node.getPrimaryItem();
                            if(item.isNode()) {
                                node = (Node) item;
                                if(node.isNodeType("hippo:resource")) {
                                    node.setProperty("jcr:mimeType", mimetype);
                                    node.setProperty("jcr:data", istream);
                                }
                                node.save();
                            }
                        } catch (MappingException ex) {
                            Gallery.log.error(ex.getMessage());
                        } catch (RepositoryException ex) {
                            Gallery.log.error(ex.getMessage());
                        }
                    }
                } catch(IOException ex) {
                    Gallery.log.info("upload of image truncated");
                }
            }
        }
    }
}
