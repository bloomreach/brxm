/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadException;
import org.hippoecm.frontend.plugins.yui.upload.MultiFileUploadDialog;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryWorkflowPlugin extends CompatibilityWorkflowPlugin<GalleryWorkflow> {
    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(GalleryWorkflowPlugin.class);

    public class UploadDialog extends MultiFileUploadDialog {
        private static final long serialVersionUID = 1L;

        public UploadDialog(IPluginContext context, IPluginConfig config) {
            super(context, config);
        }

        @Override
        public IModel<String> getTitle() {
            return new StringResourceModel(GalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""),
                    GalleryWorkflowPlugin.this, null);
        }

        @Override
        protected void handleUploadItem(FileUpload upload) throws FileUploadException {
            try {
                createGalleryItem(upload);
            } catch (GalleryException e) {
                throw new FileUploadException("Error while creating gallery item", e);
            }
        }

        @Override
        protected void onOk() {
            super.onOk();
            afterUploadItems();
        }
    }

    public String type;
    private List<String> newItems;

    public GalleryWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        newItems = new LinkedList<>();

        AbstractView<StdWorkflow> add;
        addOrReplace(add = new AbstractView<StdWorkflow>("new", createListDataProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                item.add((StdWorkflow) item.getModelObject());
            }
        });
        add.populate();
    }

    private void createGalleryItem(FileUpload upload) throws GalleryException {
        try {
            String filename = upload.getClientFileName();
            String mimetype = upload.getContentType();
            InputStream is = upload.getInputStream();

            WorkflowManager manager = UserSession.get().getWorkflowManager();
            HippoNode node;
            try {
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                        .getDefaultModel();
                GalleryWorkflow workflow = (GalleryWorkflow) manager
                        .getWorkflow(workflowDescriptorModel.getObject());
                String nodeName = getNodeNameCodec().encode(filename);
                String localName = getLocalizeCodec().encode(filename);
                Document document = workflow.createGalleryItem(nodeName, type);
                node = (HippoNode) UserSession.get().getJcrSession()
                        .getNodeByIdentifier(document.getIdentity());
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
            } catch (WorkflowException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage());
                throw new GalleryException("Workflow failed", ex);
            } catch (RepositoryException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage());
                throw new GalleryException("Repository failed", ex);
            }

            try {
                getGalleryProcessor().makeImage(node, is, mimetype, filename);
                node.getSession().save();
                newItems.add(node.getPath());
            } catch (Exception ex) {
                remove(manager, node);
                final StringResourceModel messageModel = new StringResourceModel("upload-failed-named-label",
                        GalleryWorkflowPlugin.this, null, null, filename);
                throw new GalleryException(messageModel.getString(), ex);
            }
        } catch (IOException ex) {
            GalleryWorkflowPlugin.log.info("upload of image truncated");
            throw new GalleryException(new StringResourceModel("upload-failed-label", GalleryWorkflowPlugin.this, null).getString());
        }
    }

    private void remove(final WorkflowManager manager, final HippoNode node) {
        try {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
            defaultWorkflow.delete();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            GalleryWorkflowPlugin.log.error(e.getMessage());
        }
        try {
            node.getSession().refresh(false);
        } catch (RepositoryException e) {
            // deliberate ignore
        }
    }

    private void afterUploadItems() {
        int threshold = getPluginConfig().getAsInteger("select.after.create.threshold", 1);
        if(newItems.size() <= threshold) {
            for(String path : newItems){
                select(new JcrNodeModel(path));
            }
        }
        newItems.clear();
    }

    protected GalleryProcessor getGalleryProcessor() {
        IPluginContext context = getPluginContext();
        GalleryProcessor processor = context.getService(getPluginConfig().getString("gallery.processor.id",
                "service.gallery.processor"), GalleryProcessor.class);
        if (processor != null) {
            return processor;
        }
        return new DefaultGalleryProcessor();
    }

    protected IDataProvider<StdWorkflow> createListDataProvider() {
        List<StdWorkflow> list = new LinkedList<StdWorkflow>();
        list.add(0, new WorkflowAction("add", new StringResourceModel(getPluginConfig()
                .getString("option.label", "add"), this, null, "Add")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "image-add-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return createDialog();
            }
        });
        return new ListDataProvider<>(list);
    }

    private Dialog createDialog() {
        List<String> galleryTypes = null;
        try {
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                    .getDefaultModel();
            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowDescriptorModel.getObject());
            if (workflow == null) {
                GalleryWorkflowPlugin.log.error("No gallery workflow accessible");
            } else {
                galleryTypes = workflow.getGalleryTypes();
            }
        } catch (RepositoryException | RemoteException ex) {
            GalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
        }

        Component typeComponent;
        if (galleryTypes != null && galleryTypes.size() > 1) {
            type = galleryTypes.get(0);
            typeComponent = new DropDownChoice("type", new PropertyModel(this, "type"), galleryTypes,
                    new TypeChoiceRenderer(this)).setNullValid(false).setRequired(true);
        } else if (galleryTypes != null && galleryTypes.size() == 1) {
            type = galleryTypes.get(0);
            typeComponent = new Label("type", type).setVisible(false);
        } else {
            type = null;
            typeComponent = new Label("type", "default").setVisible(false);
        }

        UploadDialog dialog = new UploadDialog(getPluginContext(), getPluginConfig());
        dialog.add(typeComponent);
        return dialog;
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        if (browser != null) {
            try {
                if (nodeModel.getNode() != null
                        && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode()
                                .isNodeType(HippoNodeType.NT_HANDLE))) {
                    browser.browse(nodeModel);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

}
