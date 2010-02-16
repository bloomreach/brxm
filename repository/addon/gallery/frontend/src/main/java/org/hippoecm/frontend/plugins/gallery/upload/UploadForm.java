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

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.plugins.gallery.Gallery;
import org.hippoecm.frontend.plugins.gallery.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.ImageUtils;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;

class UploadForm extends Form {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final UploadDialog uploadDialog;
    private final FileUploadField uploadField;
    private String type;
    private String description;

    public UploadForm(String id, UploadDialog uploadDialog) {
        super(id, uploadDialog.getModel());
        this.uploadDialog = uploadDialog;
        setMultiPart(true);
        setMaxSize(Bytes.megabytes(5));
        add(uploadField = new FileUploadField("input"));

        Node galleryNode = uploadDialog.getGalleryNode();
        if (galleryNode == null) {
            type = null;
            add(new Label("type", "Configuration error: Cannot locate gallery"));
            uploadField.setVisible(false);
        } else {
            List<String> galleryTypes = null;
            try {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(this.uploadDialog.getWorkflowCategory(), galleryNode);
                if (workflow == null) {
                    Gallery.log.error("No gallery workflow accessible");
                } else {
                    galleryTypes = workflow.getGalleryTypes();
                }
            } catch (MappingException ex) {
                Gallery.log.error(ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                Gallery.log.error(ex.getMessage(), ex);
            } catch (RemoteException ex) {
                Gallery.log.error(ex.getMessage(), ex);
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
    }

    String getType() {
        return type;
    }

    String getDescription() {
        return description;
    }

    @Override
    protected void onSubmit() {
        final FileUpload upload = uploadField.getFileUpload();
        if (upload != null) {
            try {
                String filename = upload.getClientFileName();
                String mimetype;
                mimetype = upload.getContentType();
                InputStream istream = upload.getInputStream();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                try {
                    Node galleryNode = uploadDialog.getGalleryNode();
                    GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(this.uploadDialog
                            .getWorkflowCategory(), galleryNode);
                    String nodeName = uploadDialog.getNodeNameCodec().encode(filename);
                    String localName = uploadDialog.getLocalizeCodec().encode(filename);
                    Document document = workflow.createGalleryItem(nodeName, type);
                    HippoNode node = (HippoNode) (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                    if(!node.getLocalizedName().equals(localName)) {
                        defaultWorkflow.localizeName(localName);
                    }
                    GalleryProcessor processor = ImageUtils.galleryProcessor(uploadDialog.pluginConfig);
                    processor.makeImage(node, istream, mimetype, filename);
                    node.getSession().save();
                    uploadDialog.getWizardModel().next();
                } catch (WorkflowException ex) {
                    Gallery.log.error(ex.getMessage());
                    error(new StringResourceModel("workflow-error-label", this, null).getString());
                } catch (MappingException ex) {
                    Gallery.log.error(ex.getMessage());
                    error(new StringResourceModel("workflow-error-label", this, null).getString());
                } catch (RepositoryException ex) {
                    Gallery.log.error(ex.getMessage());
                    error(new StringResourceModel("workflow-error-label", this, null).getString());
                }
            } catch (IOException ex) {
                Gallery.log.info("upload of image truncated");
                error((new StringResourceModel("upload-failed-label", this, null).getString()));
            }
        } else {
            error(new StringResourceModel("no-file-uploaded-label", this, null).getString());
        }
    }
}
