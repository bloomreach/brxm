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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryWorkflowPlugin extends FolderWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(GalleryWorkflowPlugin.class);

    class UploadDialog extends AbstractDialog {
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
                        .getModel();
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
                    String mimetype = upload.getContentType();
                    InputStream istream = upload.getInputStream();
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    try {
                        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                                .getModel();
                        GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(GalleryWorkflowPlugin.this
                                .getPluginConfig().getString("workflow.categories"), workflowDescriptorModel.getNode());
                        Document document = workflow.createGalleryItem(NodeNameCodec.encode(filename, true), type);
                        Node node = (((UserSession) Session.get())).getJcrSession().getNodeByUUID(
                                document.getIdentity());
                        Item item = node.getPrimaryItem();
                        if (item.isNode()) {
                            Node primaryChild = (Node) item;
                            if (primaryChild.isNodeType("hippo:resource")) {
                                primaryChild.setProperty("jcr:mimeType", mimetype);
                                primaryChild.setProperty("jcr:data", istream);
                            }
                            NodeDefinition[] childDefs = node.getPrimaryNodeType().getChildNodeDefinitions();
                            for (int i = 0; i < childDefs.length; i++) {
                                if (childDefs[i].getDefaultPrimaryType() != null
                                        && childDefs[i].getDefaultPrimaryType().isNodeType("hippo:resource")) {
                                    if (!node.hasNode(childDefs[i].getName())) {
                                        Node child = node.addNode(childDefs[i].getName());
                                        child.setProperty("jcr:data", primaryChild.getProperty("jcr:data").getStream());
                                        child.setProperty("jcr:mimeType", primaryChild.getProperty("jcr:mimeType")
                                                .getString());
                                        child.setProperty("jcr:lastModified", primaryChild.getProperty(
                                                "jcr:lastModified").getDate());
                                    }
                                }
                            }
                            // description = ImageInfo.analyse(filename, primaryChild.getProperty("jcr:data").getStream());
                            makeThumbnail(primaryChild, primaryChild.getProperty("jcr:data").getStream(), primaryChild
                                    .getProperty("jcr:mimeType").getString());
                            node.getSession().save();
                        }
                    } catch (MappingException ex) {
                        GalleryWorkflowPlugin.log.error(ex.getMessage());
                        error(new StringResourceModel("workflow-error-label", GalleryWorkflowPlugin.this, null)
                                .getString());
                    } catch (RepositoryException ex) {
                        GalleryWorkflowPlugin.log.error(ex.getMessage());
                        error(new StringResourceModel("workflow-error-label", GalleryWorkflowPlugin.this, null)
                                .getString());
                    }
                } catch (IOException ex) {
                    GalleryWorkflowPlugin.log.info("upload of image truncated");
                    error((new StringResourceModel("upload-failed-label", GalleryWorkflowPlugin.this, null).getString()));
                }
            } else {
                error(new StringResourceModel("no-file-uploaded-label", GalleryWorkflowPlugin.this, null).getString());
            }
        }

        private void makeThumbnail(Node node, InputStream resourceData, String mimeType) throws RepositoryException {
            if (mimeType.startsWith("image")) {
                int thumbnailSize = GalleryWorkflowPlugin.this.getPluginConfig().getInt("gallery.thumbnail.size",
                        Gallery.DEFAULT_THUMBNAIL_SIZE);
                InputStream thumbNail = ImageUtils.createThumbnail(resourceData, thumbnailSize, mimeType);
                node.setProperty("jcr:data", thumbNail);
            } else {
                node.setProperty("jcr:data", resourceData);
            }
            node.setProperty("jcr:mimeType", mimeType);
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
