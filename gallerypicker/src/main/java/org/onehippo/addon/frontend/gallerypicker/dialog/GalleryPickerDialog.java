/*
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.addon.frontend.gallerypicker.dialog;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryPickerDialog extends LinkPickerDialog {

    private static Logger log = LoggerFactory.getLogger(GalleryPickerDialog.class);

    private static final ResourceReference DIALOG_SKIN = new CssResourceReference(GalleryPickerDialog.class, "GalleryPickerDialog.css");

    IModel<List<String>> typesModel;
    private String selectedType;

    private final FileUploadValidationService validator;

    public GalleryPickerDialog(IPluginContext context, IPluginConfig config, IModel<String> model) {
        super(context, config, model);

        validator = loadFileUploadValidationService();

        Fragment fragment;
        if (config.getAsBoolean("enable.upload", false)) {
            fragment = createUploadForm();
        } else {
            fragment = new Fragment("fragment", "empty-fragment", this);
        }
        add(fragment);
    }

    private FileUploadValidationService loadFileUploadValidationService() {
        String serviceId = getPluginConfig().getString(FileUploadValidationService.VALIDATE_ID, "service.gallery.image.validation");
        FileUploadValidationService validator = getPluginContext().getService(serviceId, FileUploadValidationService.class);
        if (validator == null) {
            validator = new DefaultUploadValidationService();
            log.warn("Cannot load validation service '{}' using the interface '{}'",
                    serviceId, FileUploadValidationService.class.getName());
        }
        return validator;
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(final String id) {
        return new FeedbackPanel(id) {{
            setOutputMarkupId(true);
        }};
    }

    private Fragment createUploadForm() {
        Fragment fragment = new Fragment("fragment", "upload-fragment", this);

        //Show available gallery types
        typesModel = new LoadableDetachableModel<List<String>>() {
            @Override
            public List<String> load() {
                return getAllowedGalleryNodeTypes();
            }
        };

        Form<?> uploadForm = new Form("uploadForm") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && typesModel.getObject().size() >= 1;
            }
        };

        uploadForm.setOutputMarkupId(true);
        uploadForm.setMultiPart(true);

        final FileUploadField uploadField = new FileUploadField("uploadField");
        uploadField.setOutputMarkupId(true);

        final AjaxButton uploadButton = new AjaxButton("uploadButton", uploadForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                final FileUpload upload = uploadField.getFileUpload();
                if (upload != null) {
                    try {
                        GalleryPickerDialog.this.validate(upload);
                        createGalleryItem(target, upload);
                    } catch (FileUploadViolationException e) {
                        final String localName = getLocalizeCodec().encode(upload.getClientFileName());
                        final List<String> errors = e.getViolationMessages();
                        final String errorMessage = StringUtils.join(errors, ";");
                        String errMsg = getExceptionTranslation(e, localName, errorMessage).getObject();
                        log.warn(errMsg);
                        GalleryPickerDialog.this.error(errMsg);
                        if (log.isDebugEnabled()) {
                            log.error("Failed to validate uploading file '{}': {}", localName, errorMessage);
                        }
                    }
                } else {
                    error("Please select a file to upload");
                }
            }

            private void createGalleryItem(final AjaxRequestTarget target, final FileUpload upload) {
                try {
                    final String filename = upload.getClientFileName();
                    final String mimetype = upload.getContentType();
                    final InputStream istream = upload.getInputStream();
                    final UserSession session = UserSession.get();
                    final WorkflowManager manager = session.getWorkflowManager();
                    HippoNode node = null;
                    String localName = null;
                    try {
                        //Get the selected folder from the folderReference Service
                        Node folderNode = getFolderModel().getObject();

                        //TODO replace shortcuts with custom workflow category(?)
                        GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("shortcuts", folderNode);
                        String nodeName = getNodeNameCodec().encode(filename);
                        localName = getLocalizeCodec().encode(filename);
                        Document document = workflow.createGalleryItem(nodeName, selectedType);
                        node = (HippoNode) session.getJcrSession().getNodeByIdentifier(document.getIdentity());
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        if (!node.getLocalizedName().equals(localName)) {
                            defaultWorkflow.localizeName(localName);
                        }
                    } catch (WorkflowException | RepositoryException ex) {
                        log.error(ex.getMessage());
                        error(getExceptionTranslation(ex, localName).getObject());
                    }

                    if (node != null) {
                        try {
                            getGalleryProcessor().makeImage(node, istream, mimetype, filename);
                            node.getSession().save();
                            uploadField.setModel(null);
                            target.add(uploadField);
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                            error(getExceptionTranslation(ex));
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
                            error(getExceptionTranslation(ex));
                        }
                    }
                } catch (IOException ex) {
                    log.info("upload of image truncated");
                    error("Unable to read the uploaded image");
                }
            }
        };

        uploadButton.setOutputMarkupId(true);
        uploadField.add(new AjaxEventBehavior("onchange") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                uploadButton.setEnabled(true);
                target.add(uploadButton);
            }
        });
        uploadButton.setEnabled(false);
        uploadForm.add(uploadField);
        uploadForm.add(uploadButton);

        final DropDownChoice typeSelect = new DropDownChoice<String>(
                "type",
                new PropertyModel<>(this, "selectedType"),
                typesModel,
                new TypeChoiceRenderer(this)) {

            @Override
            public boolean isVisible() {
                return typesModel != null && typesModel.getObject().size() > 1;
            }
        };

        typeSelect.setNullValid(false).setRequired(true).setOutputMarkupId(true);

        typeSelect.add(new AjaxEventBehavior("onchange") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                //target.add(GalleryPickerDialog.this);
            }
        });
        uploadForm.add(typeSelect);

        fragment.add(uploadForm);
        //add(uploadForm);

        //OMG: ugly workaround.. Input[type=file] is rendered differently on OSX in all browsers..
        RequestCycle requestCycle = RequestCycle.get();
        HttpServletRequest httpServletReq = (HttpServletRequest) requestCycle.getRequest().getContainerRequest();
        String ua = httpServletReq.getHeader("User-Agent");
        if (ua.contains("Macintosh")) {
            uploadField.add(CssClass.append("browse-button-osx"));
            uploadButton.add(CssClass.append("upload-button-osx"));
        }
        return fragment;
    }

    /**
     * Validate upload file with file upload validation service
     *
     * @param upload File upload model
     * @throws FileUploadViolationException
     */
    private void validate(final FileUpload upload) throws FileUploadViolationException {
        try {
            validator.validate(upload);
        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
            throw new FileUploadViolationException("Error while validating upload " + e.getMessage());
        }

        IValidationResult result = validator.getValidationResult();
        if (!result.isValid()) {
            List<String> errors = result.getViolations().stream()
                    .map(violation -> violation.getMessage().getObject())
                    .collect(Collectors.toList());
            throw new FileUploadViolationException(errors);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(DIALOG_SKIN));
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

    private StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    private StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected List<String> getAllowedGalleryNodeTypes() {
        final WorkflowManager manager = UserSession.get().getWorkflowManager();
        try {
            Node folderNode = getFolderModel().getObject();
            if (!folderNode.getSession().hasPermission(folderNode.getPath(), "jcr:write")) {
                return Collections.emptyList();
            }
            //TODO replace shortcuts with custom workflow category(?)
            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("shortcuts", folderNode);
            return workflow != null ? workflow.getGalleryTypes() : Collections.<String>emptyList();

        } catch (RepositoryException | RemoteException e) {
            log.error("Error obtaining gallery types", e);
        }
        return Collections.emptyList();
    }

    @Override
    protected void onFolderSelected(final IModel<Node> model) {
        super.onFolderSelected(model);

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(GalleryPickerDialog.this);
        }
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (typesModel != null && typesModel.getObject().size() >= 1) {
            selectedType = typesModel.getObject().get(0);
        }
    }
}
