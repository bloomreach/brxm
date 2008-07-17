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

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
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
import org.hippoecm.repository.gallery.GalleryWorkflow;

public class WizardDialog extends WebPage {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private IServiceReference<IJcrService> jcrServiceRef;
    private WizardForm form;
    private String workflowCategory;
    protected AjaxLink submit;
    private IServiceReference<IDialogService> windowRef;
    private String exception = "";
    
    private static final Set<String> supportRescaleMimeTypes = new HashSet<String>();
    {
        supportRescaleMimeTypes.add("image/jpeg");
    }
  
    public WizardDialog(GalleryShortcutPlugin plugin, IPluginContext context, IPluginConfig config, IDialogService dialogWindow) {
        this.windowRef = context.getReference(dialogWindow);

        try {
            String path = config.getString("gallery.path");
            if(path != null) {
                while(path.startsWith("/"))
                    path = path.substring(1);
                setModel(new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path)));
            }
        } catch(PathNotFoundException ex) {
            // cannot occur anymore because GalleryShortcutPlugin already checked this, however
            // because of HREPTWO-1218 we cannot use the model of GalleryShortcutPlugin.
        } catch(RepositoryException ex) {
            Gallery.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
        JcrNodeModel model = (JcrNodeModel) getModel();

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel); 

        IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
        jcrServiceRef = context.getReference(service);

        workflowCategory = config.getString("gallery.workflow");
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
        if(jcrServiceRef != null) {
            jcrServiceRef.detach();
        }
        super.onDetach();
    }
    
    private class WizardForm extends Form {
        private static final long serialVersionUID = 1L;

        private final FileUploadField uploadField;

        private String type;

        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }

        public WizardForm(String name, JcrNodeModel model) {
            super(name, model);
            setMultiPart(true);
            setMaxSize(Bytes.megabytes(5));
            add(uploadField = new FileUploadField("input"));

            Node galleryNode = model.getNode();
            List<String> galleryTypes = null;
            try {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowCategory, galleryNode);
                galleryTypes = workflow.getGalleryTypes();
            } catch(MappingException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                Gallery.log.error(ex.getMessage(), ex);
            } catch(RepositoryException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                Gallery.log.error(ex.getMessage(), ex);
            } catch(RemoteException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                Gallery.log.error(ex.getMessage(), ex);
            }
            if(galleryTypes != null && galleryTypes.size() > 0) {
                DropDownChoice folderChoice;
                type = galleryTypes.get(0);
                add(folderChoice = new DropDownChoice("type", new PropertyModel(this, "type"), galleryTypes));
                folderChoice.setNullValid(false);
                folderChoice.setRequired(true);
            } else {
                Component component;
                add(component = new Label("type", "default"));
                component.setVisible(false);
                type = null;
            }
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
                            Document document = workflow.createGalleryItem(filename, type);
                            Node node = (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                            Item item = node.getPrimaryItem();
                            if(item.isNode()) {
                                Node primaryChild = (Node) item;
                                if(primaryChild.isNodeType("hippo:resource")) {
                                    primaryChild.setProperty("jcr:mimeType", mimetype);
                                    primaryChild.setProperty("jcr:data", istream);
                                }
                                NodeDefinition[] childDefs = node.getPrimaryNodeType().getChildNodeDefinitions();
                                for(int i=0; i<childDefs.length; i++) {
                                    if(childDefs[i].getDefaultPrimaryType() != null &&
                                       childDefs[i].getDefaultPrimaryType().isNodeType("hippo:resource")) {
                                        if(!node.hasNode(childDefs[i].getName())) {
                                            Node child = node.addNode(childDefs[i].getName());
                                            child.setProperty("jcr:data", primaryChild.getProperty("jcr:data").getStream());
                                            child.setProperty("jcr:mimeType", primaryChild.getProperty("jcr:mimeType").getString());
                                            child.setProperty("jcr:lastModified", primaryChild.getProperty("jcr:lastModified").getDate());
                                        }
                                    }
                                }
                                makeThumbnail(primaryChild,
                                              primaryChild.getProperty("jcr:data").getStream(),
                                              primaryChild.getProperty("jcr:mimeType").getString());
                                node.save();
                            }
                        } catch (MappingException ex) {
                            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                            ex.printStackTrace(System.err);
                            Gallery.log.error(ex.getMessage());
                            exception = "Workflow error: " + ex.getMessage();
                        } catch (RepositoryException ex) {
                            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                            ex.printStackTrace(System.err);
                            Gallery.log.error(ex.getMessage());
                            exception = "Workflow error: " + ex.getMessage();
                        }
                    }
                } catch(IOException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                    Gallery.log.info("upload of image truncated");
                    exception = "Upload failed: " + ex.getMessage();
                }
            } else {
                exception = "No file uploaded";
            }
        }
    }

    protected void makeThumbnail(Node resource, InputStream istream, String mimeType) {
        int maxWidthThumbnail = 100;
        if(supportRescaleMimeTypes.contains(mimeType)) {
            try {
                String scaledMimeType = mimeType;
                InputStream thumbStream = null;
                try {
                    thumbStream = JpegImageResizer.resizeImage(istream, maxWidthThumbnail);
                } catch (IOException e) {
                    System.err.println(e.getClass().getName()+": "+e.getMessage());
                    e.printStackTrace(System.err);
                    return;
                }
                resource.setProperty("jcr:data", thumbStream);
                resource.setProperty("jcr:mimeType", scaledMimeType);
            } catch(RepositoryException ex) {
            }
        }
    }
}
