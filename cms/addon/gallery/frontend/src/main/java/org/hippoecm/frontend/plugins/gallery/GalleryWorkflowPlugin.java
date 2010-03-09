/*
 *  Copyright 2009 Hippo.
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
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.FolderWorkflowPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryWorkflowPlugin extends FolderWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(GalleryWorkflowPlugin.class);

    public class UploadDialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        private final FileUploadField uploadField;
        public String type;

        public UploadDialog() {
            super();
            setMultiPart(true);
            setOutputMarkupId(true);
            setNonAjaxSubmit();
            add(uploadField = new FileUploadField("input"));
            setFocus(uploadField);

            List<String> galleryTypes = null;
            try {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                        .getDefaultModel();
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(GalleryWorkflowPlugin.this
                        .getPluginConfig().getString("workflow.categories"), workflowDescriptorModel.getNode());
                if (workflow == null) {
                    GalleryWorkflowPlugin.log.error("No gallery workflow accessible");
                } else {
                    galleryTypes = workflow.getGalleryTypes();
                }
            } catch (MappingException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
            } catch (RemoteException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
            }
            if (galleryTypes != null && galleryTypes.size() > 1) {
                DropDownChoice folderChoice;
                type = galleryTypes.get(0);
                add(folderChoice = new DropDownChoice("type", new PropertyModel(this, "type"), galleryTypes,
                        new TypeChoiceRenderer(this)));
                folderChoice.setNullValid(false);
                folderChoice.setRequired(true);
            } else if (galleryTypes != null && galleryTypes.size() == 1) {
                type = galleryTypes.get(0);
                Component component;
                add(component = new Label("type", type));
                component.setVisible(false);
            } else {
                type = null;
                Component component;
                add(component = new Label("type", "default"));
                component.setVisible(false);
            }
        }

        public IModel getTitle() {
            return new StringResourceModel(GalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""),
                    GalleryWorkflowPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }

        @Override
        protected void onOk() {
            final FileUpload upload = uploadField.getFileUpload();
            if (upload != null) {
                try {
                    String filename = upload.getClientFileName();
                    String mimetype;

                    mimetype = upload.getContentType();
                    InputStream istream = upload.getInputStream();
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    HippoNode node = null;
                    try {
                        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                                .getDefaultModel();
                        GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(GalleryWorkflowPlugin.this
                                .getPluginConfig().getString("workflow.categories"), workflowDescriptorModel.getNode());
                        String nodeName = getNodeNameCodec().encode(filename);
                        String localName = getLocalizeCodec().encode(filename);
                        Document document = workflow.createGalleryItem(nodeName, type);
                        node = (HippoNode) (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        if(!node.getLocalizedName().equals(localName)) {
                            defaultWorkflow.localizeName(localName);
                        }
                    } catch (WorkflowException ex) {
                        GalleryWorkflowPlugin.log.error(ex.getMessage());
                        error(ex);
                    } catch (MappingException ex) {
                        GalleryWorkflowPlugin.log.error(ex.getMessage());
                        error(ex);
                    } catch (RepositoryException ex) {
                        GalleryWorkflowPlugin.log.error(ex.getMessage());
                        error(ex);
                    }
                    if (node != null) {
                        try {
                            ImageUtils.galleryProcessor(getPluginConfig()).makeImage(node, istream, mimetype, filename);
                            node.getSession().save();
                        } catch (RepositoryException ex) {
                            GalleryWorkflowPlugin.log.error(ex.getMessage());
                            error(ex);
                            try {
                                DefaultWorkflow defaultWorkflow = (DefaultWorkflow)manager.getWorkflow("core", node);
                                defaultWorkflow.delete();
                            } catch (WorkflowException e) {
                                GalleryWorkflowPlugin.log.error(e.getMessage());
                            } catch (MappingException e) {
                                GalleryWorkflowPlugin.log.error(e.getMessage());
                            } catch (RepositoryException e) {
                                GalleryWorkflowPlugin.log.error(e.getMessage());
                            }
                            try {
                                node.getSession().refresh(false);
                            } catch(RepositoryException e) {
                                // deliberate ignore
                            }
                        }
                    }
                } catch (IOException ex) {
                    GalleryWorkflowPlugin.log.info("upload of image truncated");
                    error((new StringResourceModel("upload-failed-label", GalleryWorkflowPlugin.this, null).getString()));
                }
            } else {
                error(new StringResourceModel("no-file-uploaded-label", GalleryWorkflowPlugin.this, null).getString());
            }
        }
    }

    public GalleryWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected IDataProvider createListDataProvider(List<StdWorkflow> list) {
        list.add(0, new WorkflowAction("add", new StringResourceModel(getPluginConfig()
                .getString("option.label", "add"), this, null, "Add")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "image-add-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                UploadDialog dialog = new UploadDialog();
                return dialog;
            }
        });
        return super.createListDataProvider(list);
    }

}
