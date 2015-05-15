/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.jquery.upload.single.JQuerySingleFileUploadWidget;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
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

public class ImageBrowserDialog extends AbstractBrowserDialog<RichTextEditorImageLink> implements IHeaderContributor {

    private static final Logger log = LoggerFactory.getLogger(ImageBrowserDialog.class);

    private static final ResourceReference DIALOG_SKIN = new CssResourceReference(ImageBrowserDialog.class, "ImageBrowserDialog.css");
    private static final String CONFIG_PREFERRED_IMAGE_VARIANT = "preferred.image.variant";
    private static final String DEFAULT_PREFERRED_IMAGE_VARIANT = "hippogallery:original";
    private static final String GALLERY_TYPE_SELECTOR_ID = "galleryType";
    private static final String EXCLUDED_IMAGE_VARIANTS = "excluded.image.variants";
    private static final String INCLUDED_IMAGE_VARIANTS = "included.image.variants";

    public static final List<String> ALIGN_OPTIONS = Arrays.asList("top", "middle", "bottom", "left", "right");
    private static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";

    private final FileUploadValidationService validator;

    DropDownChoice<String> type;

    private final LinkedHashMap<String, String> nameTypeMap;
    private final IModel<RichTextEditorImageLink> imageModel;
    private boolean uploadSelected;
    private AjaxButton uploadButton;
    private Component uploadTypeSelector;
    private final LoadableDetachableModel<List<String>> galleryTypesModel;
    private String galleryType;

    private boolean okSucceeded = false;
    private JQuerySingleFileUploadWidget fileUploadWidget;
    
    public ImageBrowserDialog(IPluginContext context, final IPluginConfig config, final IModel<RichTextEditorImageLink> model) {
        super(context, config, model);

        imageModel = model;

        galleryTypesModel = new LoadableDetachableModel<List<String>>() {
            @Override
            protected List<String> load() {
                return loadGalleryTypes();
            }
        };
        validator = ImageUploadValidationService.getValidationService(getPluginConfig(), getPluginContext());

        add(createUploadForm());

        nameTypeMap = new LinkedHashMap<>();

        type = new DropDownChoice<>("type", new StringPropertyModel(model, RichTextEditorImageLink.TYPE), Collections.emptyList(), new IChoiceRenderer<String>() {

            public Object getDisplayValue(String object) {
                return nameTypeMap.get(object);
            }

            public String getIdValue(String object, int index) {
                return object;
            }

        });
        type.add(new AjaxFormComponentUpdatingBehavior("onChange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });
        type.setOutputMarkupId(true);

        // disable the type selector to prevent Wicket from initializing the drop down incorrectly
        // when a new image is uploaded (the form submit of the upload sets the model to null)
        type.setEnabled(false);

        add(type);

        add(new ThrottledTextFieldWidget("alt", new StringPropertyModel(model, RichTextEditorImageLink.ALT)) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        DropDownChoice<String> align = new DropDownChoice<>("align", new StringPropertyModel(model,
                RichTextEditorImageLink.ALIGN), ALIGN_OPTIONS, new IChoiceRenderer<String>() {

            public Object getDisplayValue(String object) {
                return new StringResourceModel(object, ImageBrowserDialog.this, null).getString();
            }

            public String getIdValue(String object, int index) {
                return object;
            }

        });
        align.add(new AjaxFormComponentUpdatingBehavior("onChange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        align.setOutputMarkupId(true);
        align.setNullValid(false);
        add(align);

        checkState();
    }

    private Component createUploadForm() {
        final HippoForm uploadForm = new HippoForm("uploadForm");
        uploadForm.setOutputMarkupId(true);

        // we use a container to enable Ajax-based (in)visibility while not meddling with the selected upload file.
        uploadTypeSelector = new WebMarkupContainer("uploadTypeSelector").add(createTypeSelector());
        uploadTypeSelector.setOutputMarkupId(true);
        uploadForm.add(uploadTypeSelector);

        uploadButton = new AjaxButton("uploadButton", new StringResourceModel("button-upload-label", this, null)){
            @Override
            protected String getOnClickScript(){
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
                    return galleryTypes.size() > 0; // disable upload if the current folder has no gallery types at all
                }
                return false;
            }
        };
        uploadButton.setOutputMarkupId(true);
        uploadForm.add(this.uploadButton);

        fileUploadWidget = new JQuerySingleFileUploadWidget(FILEUPLOAD_WIDGET_ID, getPluginConfig(), validator) {
            @Override
            protected void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                clearOldFeedbackMessages();
                createGalleryItem(fileUpload);
            }

            @Override
            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                clearOldFeedbackMessages();

                final List<String> errorMessages = fileUploadInfo.getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    errorMessages.forEach(ImageBrowserDialog.this::error);
                    log.error("file {} contains errors: {}", fileUploadInfo.getFileName(), StringUtils.join(errorMessages, ";"));
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

        //OMG: ugly workaround.. Input[type=file] is rendered differently on OSX in all browsers..
        HttpServletRequest httpServletReq = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        String ua = httpServletReq.getHeader("User-Agent");
        if (ua.contains("Macintosh")) {
            uploadButton.add(new AttributeAppender("class", new Model<>("upload-button-osx")));
        }

        return uploadForm;
    }

    private void clearOldFeedbackMessages() {
        if (ImageBrowserDialog.this.hasFeedbackMessage()) {
            ImageBrowserDialog.this.getFeedbackMessages().clear();
        }
    }

    protected void onModelSelected(IModel<Node> model) {
        type.setEnabled(true);

        setTypeChoices(model.getObject());

        if (type != null) {
            // if an preexisting image is selected, the constructor is not yet called so the class members are not initialized
            setPreferredTypeChoice();
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(type);
            }
        }
        super.onModelSelected(model);
    }

    /**
     * This is the callback to enable/disable the OK button of the image browser dialog. We abuse it here as a signal
     * that a new folder may have been selected. If so, the drop-down menu for selecting the target gallery type for
     * uploaded images may need to be adjusted.
     *
     * @param isset pass-through parameter
     */
    @Override
    protected void setOkEnabled(boolean isset) {
        super.setOkEnabled(isset);

        // function is called by parent constructor, when our constructor hasn't run yet...
        if (uploadTypeSelector != null && uploadButton != null) {

            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(uploadTypeSelector);
                target.add(uploadButton);
            }
        }
    }

    private void setPreferredTypeChoice() {
        final IPluginConfig config = getPluginConfig();
        String preferredType = config.getString(CONFIG_PREFERRED_IMAGE_VARIANT, DEFAULT_PREFERRED_IMAGE_VARIANT);
        if (nameTypeMap.containsKey(preferredType)) {
            imageModel.getObject().setType(preferredType);
        }
        if (StringUtils.isBlank(imageModel.getObject().getType())) {
            log.warn("Unknown preferred image variant: '" + preferredType + "'. "
                    + "Set the property '" + CONFIG_PREFERRED_IMAGE_VARIANT + "' to one of the available variants: " + nameTypeMap.keySet());
            if (nameTypeMap.size() > 0) {
                String firstType = nameTypeMap.keySet().iterator().next();
                imageModel.getObject().setType(firstType);
            }
        }
    }

    private void setTypeChoices(final Node imageSetNode) {
        nameTypeMap.clear();

        Set<Map.Entry<String, String>> sortedEntries = new TreeSet<>(new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(final Map.Entry<String, String> o1, final Map.Entry<String, String> o2) {
                return (o1.getValue() == null || o2.getValue() == null) ? -1 : o1.getValue().compareTo(o2.getValue());
            }
        });

        try {
            Node tmpImageSetNode = imageSetNode;
            if (tmpImageSetNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                tmpImageSetNode = tmpImageSetNode.getNode(tmpImageSetNode.getName());
            }
            TypeTranslator typeTranslator = new TypeTranslator(new JcrNodeTypeModel(tmpImageSetNode.getPrimaryNodeType()));
            NodeIterator childNodes = tmpImageSetNode.getNodes();
            List<String> allImageVariants = getAllImageVariants(childNodes);
            List<String> shownImageVariants = ShownImageVariantsBuilder.getAllowedList(allImageVariants, getExcludedImageVariants(), getIncludedImageVariants());
            for (String childNodeName : shownImageVariants) {
                sortedEntries.add(new AbstractMap.SimpleEntry<>(childNodeName, typeTranslator.getPropertyName(childNodeName).getObject()));
            }
        } catch (RepositoryException repositoryException) {
            log.error("Error updating the available image variants.", repositoryException);
        }

        for (Map.Entry<String, String> entry : sortedEntries) {
            nameTypeMap.put(entry.getKey(), entry.getValue());
        }

        if (type != null) {
            type.setChoices(new ArrayList<>(nameTypeMap.keySet()));
            type.updateModel();
        }
    }

    private List<String> getAllImageVariants(final NodeIterator childNodes) throws RepositoryException {
        List<String> allImageVariants = new ArrayList<>();
        while (childNodes.hasNext()) {
            Node childNode = childNodes.nextNode();
            if (childNode.isNodeType("hippogallery:image")) {
                String childNodeName = childNode.getName();
                allImageVariants.add(childNodeName);
            }
        }
        return allImageVariants;
    }


    private List<String> getIncludedImageVariants() {
        return getMultipleString(INCLUDED_IMAGE_VARIANTS);
    }

    private List<String> getExcludedImageVariants() {
        return getMultipleString(EXCLUDED_IMAGE_VARIANTS);
    }

    private List<String> getMultipleString(final String key) {
        List<String> result = new ArrayList<>();
        IPluginConfig pluginConfig = getPluginConfig();
        if (!pluginConfig.containsKey(key)) {
            return null;
        }
        String[] stringArray = pluginConfig.getStringArray(key);
        Collections.addAll(result, stringArray);
        return result;
    }

    private void createGalleryItem(final FileUpload upload) {
        try {
            String filename = upload.getClientFileName();
            String mimetype;

            mimetype = upload.getContentType();
            InputStream istream = upload.getInputStream();
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            HippoNode node = null;
            String localName = null;
            try {
                //Get the selected folder from the folderReference Service
                Node folderNode = getFolderModel().getObject();

                //TODO replace shortcuts with custom workflow category(?)
                String nodeName = getNodeNameCodec(folderNode).encode(filename);
                localName = getLocalizeCodec().encode(filename);
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
                Document document = workflow.createGalleryItem(nodeName, getGalleryType());
                node = (HippoNode) UserSession.get().getJcrSession().getNodeByIdentifier(document.getIdentity());
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
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    fileUploadWidget.error(getExceptionTranslation(ex));
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
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null){
            target.add(feedback);
        }
    }

    @Override
    protected void onOk() {
        RichTextEditorImageLink image = getModelObject();

        if (image.isValid()) {
            image.save();
            okSucceeded = true;
        } else {
            okSucceeded = false;
            error("Please select an image");
        }
    }

    @Override
    public void onClose() {
        if (!okSucceeded) {
            cancelled = true;
        }
        super.onClose();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(DIALOG_SKIN));
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=855,height=525");
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

    private StringCodec getNodeNameCodec(Node node) {
        return CodecUtils.getNodeNameCodec(getPluginContext(), node);
    }

    private StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
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
     * Create the galleryTypeSelector, only shown in the UI if there actually is something to choose from. Send changes
     * to the backend using Ajax, in order to remember old choices while navigating through the gallery.
     *
     * @return the type selector component
     */
    @SuppressWarnings("unchecked")
    private Component createTypeSelector() {
        getGalleryType(); // initialize the galleryType value
        return new DropDownChoice<String>(GALLERY_TYPE_SELECTOR_ID, new PropertyModel(this, "galleryType"),
                galleryTypesModel, new TypeChoiceRenderer(this)) {
            @Override
            public boolean isVisible() {
                return getChoices().size() > 1;
            }
        }
                .setNullValid(false)
                .add(new AjaxFormComponentUpdatingBehavior("onchange") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // required because abstract, but all we need is to have galleryType set, which happens underwater.
                    }
                });
    }

    /**
     * Load gallery types from repo-based configuration (target folder)
     *
     * @return list of supported type names for the current folder.
     */
    private List<String> loadGalleryTypes() {
        List<String> types = new ArrayList<>();
        WorkflowManager manager = UserSession.get().getWorkflowManager();

        try {
            Node folderNode = getFolderModel().getObject();
            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
            types = workflow.getGalleryTypes();
        } catch (Exception e) {
            log.error("Failed to load gallery types", e);
        }

        return types;
    }
}
