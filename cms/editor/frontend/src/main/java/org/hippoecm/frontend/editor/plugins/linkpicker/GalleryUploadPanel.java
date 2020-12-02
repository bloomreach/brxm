/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.i18n.TranslatorUtils;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.SvgOnLoadGalleryException;
import org.hippoecm.frontend.plugins.gallery.model.SvgScriptGalleryException;
import org.hippoecm.frontend.plugins.jquery.upload.AbstractFileUploadWidget;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.jquery.upload.single.SingleFileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

/**
 * Panel of a single file upload form, which is used to upload image to the gallery.
 * The panel's model holds the node representing the folder into which the image is about to be uploaded.
 */
public abstract class GalleryUploadPanel extends Panel {
    private static final Logger log = LoggerFactory.getLogger(GalleryUploadPanel.class);

    private static final String SVG_MIME_TYPE = "image/svg+xml";
    private final String SVG_SCRIPTS_ENABLED = "svg.scripts.enabled";

    private static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";
    private final IPluginContext context;

    private AjaxButton uploadButton;
    private final LoadableDetachableModel<List<String>> galleryTypesModel;
    private final FileUploadValidationService validator;
    private final FileUploadPreProcessorService fileUploadPreProcessorService;
    private AbstractFileUploadWidget fileUploadWidget;

    private boolean uploadSelected;
    private String galleryType;
    private GalleryProcessor galleryProcessor;

    private final IPluginConfig pluginConfig;

    public GalleryUploadPanel(final String id, final IModel<Node> model,
                              final IPluginContext context, final IPluginConfig config,
                              GalleryProcessor galleryProcessor) {
        super(id, model);
        this.pluginConfig = config;
        this.context = context;
        this.galleryProcessor = galleryProcessor;

        galleryTypesModel = new LoadableDetachableModel<List<String>>() {
            @Override
            protected List<String> load() {
                return loadGalleryTypes();
            }
        };
        validator = ImageUploadValidationService.getValidationService(context, config);
        fileUploadPreProcessorService = DefaultFileUploadPreProcessorService.getPreProcessorService(context, config);

        add(createUploadForm(config));
        setOutputMarkupId(true);
    }

    private Component createUploadForm(final IPluginConfig config) {
        final HippoForm uploadForm = new HippoForm("uploadForm");
        uploadForm.setOutputMarkupId(true);
        uploadForm.add(new WebMarkupContainer("uploadTypeSelector").add(createTypeSelector()));

        uploadButton = new AjaxButton("uploadButton", new StringResourceModel("button-upload-label", this)) {
            @Override
            protected String getOnClickScript() {
                return fileUploadWidget.getUploadScript();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                uploadSelected = false;
                target.add(this);
            }

            @Override
            public boolean isEnabled() {
                if (uploadSelected) {
                    List<String> galleryTypes = galleryTypesModel.getObject();
                    // disable upload if the current folder does not have any gallery type
                    return galleryTypes.size() > 0;
                }
                return false;
            }
        };
        uploadButton.setOutputMarkupId(true);
        uploadForm.add(this.uploadButton);

        fileUploadWidget = new SingleFileUploadWidget(FILEUPLOAD_WIDGET_ID, config, validator,
                fileUploadPreProcessorService) {
            @Override
            protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
                // because it's an ajax event, feedbacks need to be removed manually
                clearOldFeedbackMessages();
            }

            @Override
            protected void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                createGalleryItem(fileUpload, getGalleryType());
            }

            @Override
            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                final List<String> errorMessages = fileUploadInfo.getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    errorMessages.forEach(GalleryUploadPanel.this::error);
                    log.debug("file {} contains errors: {}", fileUploadInfo.getFileName(), StringUtils.join(errorMessages, ";"));
                }
            }

            @Override
            protected void onChange(final AjaxRequestTarget target) {
                uploadSelected = true;
                if (uploadButton != null) {
                    target.add(uploadButton);
                }
            }
        };
        uploadForm.add(this.fileUploadWidget);

        return uploadForm;
    }

    protected void createGalleryItem(final FileUpload upload, final String galleryType) {
        try {
            String filename = upload.getClientFileName();
            String mimetype;

            mimetype = upload.getContentType();
            InputStream istream = upload.getInputStream();
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            HippoNode node = null;
            String localName = null;
            try {

                final boolean svgScriptsEnabled = pluginConfig.getAsBoolean(SVG_SCRIPTS_ENABLED, false);
                if (!svgScriptsEnabled && Objects.equals(mimetype, SVG_MIME_TYPE)) {
                    final String svgContent = new String(upload.getBytes());
                    if (StringUtils.containsIgnoreCase(svgContent, "<script")) {
                        IOUtils.closeQuietly(istream);
                        throw new SvgScriptGalleryException("SVG images with embedded script are not supported.");
                    }
                    if (StringUtils.containsIgnoreCase(svgContent, "onload=")) {
                        IOUtils.closeQuietly(istream);
                        throw new SvgOnLoadGalleryException("SVG images with onload attribute are not supported.");
                    }
                }

                //Get the selected folder from the folderReference Service
                Node folderNode = (Node) getDefaultModelObject();

                //TODO replace shortcuts with custom workflow category(?)
                String nodeName = CodecUtils.getNodeNameCodec(context, folderNode).encode(filename);
                localName = CodecUtils.getDisplayNameCodec(context).encode(filename);
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
                Document document = workflow.createGalleryItem(nodeName, galleryType, filename);
                node = (HippoNode) UserSession.get().getJcrSession().getNodeByIdentifier(document.getIdentity());
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getDisplayName().equals(localName)) {
                    defaultWorkflow.setDisplayName(localName);
                }
            } catch (WorkflowException | SvgScriptGalleryException | SvgOnLoadGalleryException | RepositoryException ex) {
                log.error(ex.getMessage());
                error(TranslatorUtils.getExceptionTranslation(GalleryUploadPanel.class, ex, localName).getObject());
            }
            if (node != null) {
                try {
                    galleryProcessor.makeImage(node, istream, mimetype, filename);
                    node.getSession().save();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    error(TranslatorUtils.getExceptionTranslation(GalleryUploadPanel.class, ex));
                    try {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        defaultWorkflow.delete();
                    } catch (WorkflowException | RepositoryException e) {
                        log.error(e.getMessage());
                    }
                    try {
                        node.getSession().refresh(false);
                    } catch (RepositoryException e) {
                        // deliberate ignore
                    }
                } catch (GalleryException ex) {
                    log.error(ex.getMessage());
                    error(TranslatorUtils.getExceptionTranslation(GalleryUploadPanel.class, ex));
                }
            }
        } catch (IOException ex) {
            log.info("upload of image truncated");
            error("Unable to read the uploaded image");
        }
    }

    /**
     * Manually clear feedback messages
     */
    private void clearOldFeedbackMessages() {
        if (hasFeedbackMessage()) {
            getFeedbackMessages().clear();
        }
    }

    /**
     * Create the galleryTypeSelector, only shown in the UI if there actually is something to choose from. Send changes
     * to the backend using Ajax, in order to remember old choices while navigating through the gallery.
     *
     * @return the type selector component
     */
    @SuppressWarnings("unchecked")
    private Component createTypeSelector() {
        getGalleryType(); // initialize the galleryType value
        return new DropDownChoice<String>("gallery-type-selector", new PropertyModel(this, "galleryType"),
                galleryTypesModel,
                new TypeChoiceRenderer(this)) {
            @Override
            public boolean isVisible() {
                return getChoices().size() > 1;
            }
        }
                .setNullValid(false)
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // required because abstract, but all we need is to have galleryType set, which happens underwater.
                    }
                });
    }

    private String getGalleryType() {
        List<String> galleryTypes = galleryTypesModel.getObject();

        if (galleryType == null || galleryTypes.indexOf(galleryType) < 0) {
            if (galleryTypes.size() > 0) {
                galleryType = galleryTypes.get(0);
            }
        }
        return galleryType;
    }

    /**
     * Load gallery types from repo-based configuration (target folder)
     *
     * @return list of supported type names for the current folder.
     */
    private List<String> loadGalleryTypes() {
        List<String> types = Collections.emptyList();
        WorkflowManager manager = UserSession.get().getWorkflowManager();

        try {
            Node folderNode = (Node) getDefaultModel().getObject();
            if (folderNode != null && folderNode.getSession().hasPermission(folderNode.getPath(), HIPPO_AUTHOR)) {
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
                if (workflow != null) {
                    types = workflow.getGalleryTypes();
                }
            }
        } catch (RepositoryException | RemoteException e) {
            log.error("Error obtaining gallery types", e);
        }

        return types;
    }
}
