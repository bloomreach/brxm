/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.upload.FileUploadException;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.SvgOnLoadGalleryException;
import org.hippoecm.frontend.plugins.gallery.model.SvgScriptGalleryException;
import org.hippoecm.frontend.plugins.jquery.upload.multiple.JQueryFileUploadDialog;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack.Position;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryWorkflowPlugin extends CompatibilityWorkflowPlugin<GalleryWorkflow> {
    private static final long serialVersionUID = 1L;

    private static final String SVG_MIME_TYPE = "image/svg+xml";
    private final String SVG_SCRIPTS_ENABLED = "svg.scripts.enabled";

    private static final Logger log = LoggerFactory.getLogger(GalleryWorkflowPlugin.class);

    public class UploadDialog extends JQueryFileUploadDialog {
        private static final long serialVersionUID = 1L;

        protected UploadDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig) {
            super(pluginContext, pluginConfig);
        }

        @Override
        public IModel<String> getTitle() {
            return new StringResourceModel(GalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""),
                    GalleryWorkflowPlugin.this, null);
        }

        @Override
        protected void onFileUpload(final FileUpload file) throws FileUploadException {
            try {
                createGalleryItem(file);
            } catch (GalleryException e) {
                throw new FileUploadException("Error while creating gallery item", e);
            }
        }

        @Override
        public void onClose() {
            afterUploadItems();
            super.onClose();
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
        try (InputStream is = upload.getInputStream()) {
            String fileName = upload.getClientFileName();
            String mimeType = upload.getContentType();

            WorkflowManager manager = UserSession.get().getWorkflowManager();
            HippoNode node;
            try {

                final boolean svgScriptsEnabled = GalleryWorkflowPlugin.this.getPluginConfig()
                        .getAsBoolean(SVG_SCRIPTS_ENABLED, false);
                if (!svgScriptsEnabled && Objects.equals(mimeType, SVG_MIME_TYPE)) {
                    final String svgContent = new String(upload.getBytes());
                    if (StringUtils.containsIgnoreCase(svgContent, "<script")) {
                        throw new SvgScriptGalleryException("SVG images with embedded script are not supported.");
                    }
                    if (StringUtils.containsIgnoreCase(svgContent, "onload=")) {
                        throw new SvgOnLoadGalleryException("SVG images with onload attribute are not supported.");
                    }
                }

                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) GalleryWorkflowPlugin.this
                        .getDefaultModel();

                GalleryWorkflow workflow = (GalleryWorkflow) manager
                        .getWorkflow(workflowDescriptorModel.getObject());

                String nodeName = getNodeNameCodec(workflowDescriptorModel.getNode()).encode(fileName);
                String localName = getLocalizeCodec().encode(fileName);
                Document document = workflow.createGalleryItem(nodeName, type, fileName);
                node = (HippoNode) UserSession.get().getJcrSession()
                        .getNodeByIdentifier(document.getIdentity());
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getDisplayName().equals(localName)) {
                    defaultWorkflow.setDisplayName(localName);
                }
            } catch (WorkflowException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage());
                throw new GalleryException("Workflow failed", ex);
            } catch (RepositoryException ex) {
                GalleryWorkflowPlugin.log.error(ex.getMessage());
                throw new GalleryException("Repository failed", ex);
            }

            try {
                GalleryProcessor galleryProcessor = DefaultGalleryProcessor.getGalleryProcessor(getPluginContext(), getPluginConfig());
                galleryProcessor.makeImage(node, is, mimeType, fileName);
                node.getSession().save();
                onGalleryItemCreation(node);
                newItems.add(node.getPath());
            } catch (Exception ex) {
                remove(manager, node);
                final StringResourceModel messageModel = new StringResourceModel("upload-failed-named-label",
                        GalleryWorkflowPlugin.this, null, null, fileName);
                throw new GalleryException(messageModel.getString(), ex);
            }
        } catch (IOException ex) {
            GalleryWorkflowPlugin.log.info("upload of image truncated");
            throw new GalleryException(new StringResourceModel("upload-failed-label", GalleryWorkflowPlugin.this, null).getString());
        }
    }

    protected void onGalleryItemCreation(Node node) {
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
        if (newItems.size() <= threshold) {
            for (String path : newItems) {
                select(new JcrNodeModel(path));
            }
        }
        newItems.clear();
    }

    protected IDataProvider<StdWorkflow> createListDataProvider() {
        final String option = getPluginConfig().getString("option.label", "add");
        final String label = getString(option, null, "Add");

        final List<StdWorkflow> list = new LinkedList<>();
        list.add(new WorkflowAction("add", Model.of(label)) {

            @Override
            protected Component getIcon(final String id) {
                HippoIconStack iconStack = new HippoIconStack(id, IconSize.M);
                iconStack.addFromSprite(option.equals("add-image") ? Icon.FILE_IMAGE : Icon.FILE);
                iconStack.addFromCms(CmsIcon.OVERLAY_PLUS, IconSize.M, Position.TOP_LEFT);
                return iconStack;
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
            typeComponent = new DropDownChoice<>("type", new PropertyModel<>(this, "type"), galleryTypes,
                    new TypeChoiceRenderer(this)).setNullValid(false).setRequired(true);
            // needed to keep dropdown selection:
            typeComponent.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                @Override
                protected void onUpdate(AjaxRequestTarget art) {
                }
            });
        } else if (galleryTypes != null && galleryTypes.size() == 1) {
            type = galleryTypes.get(0);
            typeComponent = new Label("type", type).setVisible(false);
        } else {
            type = null;
            typeComponent = new Label("type", "default").setVisible(false);
        }

        AbstractDialog dialog = newUploadDialog();
        dialog.add(typeComponent);
        return dialog;
    }

    /**
     * Override this method to extend uploading dialog
     */
    protected AbstractDialog newUploadDialog() {
        return new UploadDialog(getPluginContext(), getPluginConfig());
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    protected StringCodec getNodeNameCodec(final Node node) {
        return CodecUtils.getNodeNameCodec(getPluginContext(), node);
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
