/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.gallery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Strings;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.gallery.model.ImageModel;
import org.onehippo.cms7.essentials.dashboard.gallery.model.TranslationModel;
import org.onehippo.cms7.essentials.dashboard.ui.PluginFeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GalleryUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: GalleryPlugin.java 169748 2013-07-05 12:03:01Z dvandiepen $"
 */
public class GalleryPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(GalleryPlugin.class);

    private static final String IMAGE_SET_CONTAINER = "imagesetContainer";

    final ImageListView listView;
    private final TextField<String> nameText;
    private final TextField<String> prefixText;
    private final DropDownChoice<String> imageSetChoice;
    private final List<ImageModel> images;
    private final WebMarkupContainer imagesetContainer;
    private final FeedbackPanel feedback;
    private String name;
    private String prefix;
    private String imageNodePath;
    private String selectedImageSet;

    public GalleryPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        //############################################
        // FEEDBACK
        //############################################
        feedback = new PluginFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        //############################################
        // FORM
        //############################################

        final Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);
        imagesetContainer = new WebMarkupContainer(IMAGE_SET_CONTAINER);
        imagesetContainer.setOutputMarkupId(true);
        form.add(imagesetContainer);

        imageSetChoice = new DropDownChoice<String>("imagesets", new PropertyModel<String>(this, "selectedImageSet"), createImageSetTypesModel());
        imageSetChoice.setOutputMarkupId(true);
        form.add(imageSetChoice);
        form.add(new LoadImageSetButton("loadImageSet"));


        name = "";
        nameText = new TextField<>("name", new Model<>(name));
        prefixText = new TextField<>("prefix", new Model<>(prefix));
        images = new ArrayList<>();

        imagesetContainer.add(nameText);
        imagesetContainer.add(prefixText);
        // add listview:

        listView = new ImageListView("images", images);

        listView.modelChanged();

        listView.setOutputMarkupId(true);
        imagesetContainer.add(listView);

        imagesetContainer.add(new SubmitButton("submit"));
        imagesetContainer.add(new CopyImageSetButton("copyImageSet"));
        imagesetContainer.add(new AddButton("add-imagetype"));
        add(form);
    }

    private LoadableDetachableModel<List<String>> createImageSetTypesModel() {

        return new LoadableDetachableModel<List<String>>() {

            @Override
            protected List<String> load() {
                return getImageSetTypes();
            }
        };
    }

    /**
     * Return a list of imageset document types that are registered in the repository.
     *
     * @return list of imageset types.
     */
    private List<String> getImageSetTypes() {
        try {
            return CndUtils.getNodeTypesOfType(getContext(), HippoGalleryNodeType.IMAGE_SET, false);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve node types", e);
        }
        return Collections.emptyList();
    }

    /**
     * Load an image set on the page.
     *
     * @param imageSet the name of the image set
     * @param target   the ajax request target
     */
    private void loadImageSet(final String imageSet, final AjaxRequestTarget target) {
        loadImageSet(HippoNodeUtils.getPrefixFromType(imageSet), HippoNodeUtils.getNameFromType(imageSet), target);
    }

    /**
     * Load an image set on the page.
     *
     * @param prefix the imageset type prefix
     * @param name   the imageset type name
     * @param target the ajax request target
     */
    private void loadImageSet(final String prefix, final String name, final AjaxRequestTarget target) {
        try {
            final Session session = getJCRSession();
            imageNodePath = GalleryUtils.getNamespacePathForImageset(prefix, name);

            populateTypes(session, HippoNodeUtils.getNode(session, imageNodePath));
            addSuccess("Image set " + prefix + ':' + name + " loaded", target);
            target.add(imagesetContainer);
        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            resetSession();
            addError(e.getMessage(), target);
        }
    }

    private void copyImageSet(final String prefix, final String name, final AjaxRequestTarget target) {
        // TODO make it copy only
        createOrLoadImageSet(prefix, name, null, target);
    }

    private void createOrLoadImageSet(final String prefix, final String name, final AjaxRequestTarget target) {
        createOrLoadImageSet(prefix, name, null, target);
    }

    private void createOrLoadImageSet(final String prefix, final String name, final String imageNodePathToCopy, final AjaxRequestTarget target) {
        try {
            this.name = name;
            this.prefix = prefix;
            final String nodeType = prefix + ':' + name;
            final String uri = GalleryUtils.getGalleryURI(prefix);

            // Check whether node type already exists
            if(CndUtils.existsNodeType(getContext(), nodeType)) {
                if(CndUtils.isNodeType(getContext(), nodeType, HippoGalleryNodeType.IMAGE_SET)) {
                    // Node type exists and is and image set which can be loaded
                    loadImageSet(nodeType, target);
                    addSuccess("Image set already exists", target);
                    return;
                } else {
                    // Node type exists and is no image set
                    addError("Document type already exists, but is no image set", target);
                    return;
                }
            }

            // Check whether namespace needs to / can be created
            if(CndUtils.existsNamespacePrefix(getContext(), prefix)) {
                // No need to register namespace because it already exists
                addSuccess("Namespace prefix '" + prefix + "' already exists", target);
            } else if(CndUtils.existsNamespaceUri(getContext(), uri)) {
                // Unable to register namespace for already existing URI
                addError("Namespace URI '" + uri + "' already exists", target);
                return;
            } else {
                // Register new namespace
                CndUtils.registerNamespace(getContext(), prefix, uri);
            }

            CndUtils.createHippoNamespace(getContext(), prefix);
            CndUtils.registerDocumentType(getContext(), prefix, name, false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);

            // copy node:
            final Node imageNode;
            if (imageNodePathToCopy != null) {
                // Copy a previously used image variant
                imageNode = GalleryUtils.createImagesetNamespace(getJCRSession(), prefix, name, imageNodePath);
            } else {
                // Copy the default original image variant
                imageNode = GalleryUtils.createImagesetNamespace(getJCRSession(), prefix, name);
            }
            imageNodePath = imageNode.getPath();

            final Session session = getJCRSession();
            populateTypes(session, imageNode);
            target.add(imagesetContainer);
            addSuccess("Successfully added:  " + GalleryUtils.getImagesetName(prefix, name) + " image set.", target);

            session.save();

            // Select the created imageset in the dropdown
            this.selectedImageSet = prefix + ':' + name;

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            resetSession();
            addError(e.getMessage(), target);
        }
    }

    /**
     * Retrieve the JCR session from the Wicket context.
     *
     * @return JCR session
     */
    private Session getJCRSession() {
        return getContext().getSession();
    }

    private boolean hasImageNode() {
        if (StringUtils.isBlank(imageNodePath)) {
            return false;
        }
        try {
            if (getJCRSession().nodeExists(imageNodePath)) {
                return true;
            }
        } catch (RepositoryException e) {
            log.warn("Error when determining image node");
        }
        return false;
    }

    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    private void resetSession() {
        try {
            getJCRSession().refresh(false);
        } catch (RepositoryException e) {
            log.error("Error refreshing session", e);
        }
    }

    private void populateTypes(final Session session, final Node imagesetTemplate) throws RepositoryException {
        images.clear();
        if (imagesetTemplate == null) {
            return;
        }
        for (final Node variant : GalleryUtils.getFieldVariantsFromTemplate(imagesetTemplate)) {
            final String prefix = HippoNodeUtils.getPrefixFromType(HippoNodeUtils.getStringProperty(variant, HippoNodeUtils.HIPPOSYSEDIT_PATH));
            if (prefix != null) {
                final ImageModel model = new ImageModel(prefix);
                model.setName(variant.getName());

                // Get values from gallery processor variant
                final Node processorVariant = GalleryUtils.getGalleryProcessorVariant(session, model.getType());
                if (processorVariant != null) {
                    model.setHeight(HippoNodeUtils.getLongProperty(processorVariant, "height", Long.valueOf(0L)).intValue());
                    model.setWidth(HippoNodeUtils.getLongProperty(processorVariant, "width", Long.valueOf(0L)).intValue());
                }

                // Retrieve and set the translations to the model
                model.setTranslations(retrieveTranslationsForVariant(imagesetTemplate, model.getType()));

                images.add(model);
            }
        }
    }

    private void onLoadImageSetButton(final AjaxRequestTarget target) {
        log.info("Loading image set....text value:  [{}]", selectedImageSet);
        if (StringUtils.isBlank(selectedImageSet)) {
            addError("Unable load image set", target);
        } else {
            loadImageSet(selectedImageSet, target);
        }
    }

    private void onFormSubmit(final AjaxRequestTarget target) {
        final String myPrefix = prefixText.getModel().getObject();
        final String myName = nameText.getModel().getObject();

        log.info("Submitting form....text value:  [{}]", myName);

        if (StringUtils.isBlank(myPrefix)) {
            addError("Unable to register CND: prefix required", target);
        } else if (StringUtils.isBlank(myName)) {
            addError("Unable to register CND: name required", target);
        } else {
            createOrLoadImageSet(myPrefix, myName, target);
        }
        imageSetChoice.detach();
        target.add(imageSetChoice);
    }

    private void onCopyImageSetSubmit(final AjaxRequestTarget target) {
        final String myPrefix = prefixText.getModel().getObject();
        final String myName = nameText.getModel().getObject();

        log.info("Submitting form....text value:  [{}]", myName);

        if (StringUtils.isBlank(myPrefix)) {
            addError("Unable to register CND: prefix required", target);
        } else if (StringUtils.isBlank(myName)) {
            addError("Unable to register CND: name required", target);
        } else if (StringUtils.isBlank(imageNodePath)) {
            addError("Unable to copy image set: select an image set", target);
        } else {
            createOrLoadImageSet(myPrefix, myName, imageNodePath, target);
        }
        imageSetChoice.detach();
        target.add(imageSetChoice);
    }



    private void onAddButton(final AjaxRequestTarget target) {
        log.info("Adding type..{}", target);
        images.add(newImageModel(prefix));
        target.add(imagesetContainer);

    }

    private ImageModel newImageModel(final String prefix) {
        final ImageModel myModel = new ImageModel(prefix);
        myModel.addTranslation("nl", StringUtils.EMPTY);
        myModel.addTranslation("en", StringUtils.EMPTY);
        myModel.addTranslation("de", StringUtils.EMPTY);
        myModel.addTranslation("fr", StringUtils.EMPTY);
        myModel.addTranslation("it", StringUtils.EMPTY);
        return myModel;
    }

    private void onAddImageTranslationButton(final AjaxRequestTarget target, ImageModel model) {
        log.info("Adding translation..{}", target);
        model.addTranslation(new TranslationModel());
        target.add(imagesetContainer);

    }

    private void onRemoveImageTranslationButton(final AjaxRequestTarget target, final ImageModel model, final TranslationModel translation) {
        log.info("Removing translation..{}", target);
        if (model.isReadOnly()) {
            addError("Readonly node", target);
            return;
        }

        // TODO mark for deletion
/*
        translation.setDelete(true);
*/

/*
        try {
            final Session session = getJCRSession();

            deleteTranslationsForVariant();

            final Node imagesetTemplate = HippoNodeUtils.getNode(session, imageNodePath);

            TranslationUtils.getTranslationsFromNode(imagesetTemplate, )

            removeImageVariant(session, imagesetTemplate, oldVariant, model.getName());
            session.save();
            addSuccess("Removed image variant:  " + model.getName(), target);

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            resetSession();
            addError(e.getMessage(), target);
            return;
        }
*/


        model.removeTranslation(translation);
        target.add(imagesetContainer);
    }

    private void onRemoveButton(final AjaxRequestTarget target, final ImageModel model) {
        log.info("Removing type..{}", target);
        if (model.isReadOnly()) {
            addError("Readonly node", target);
            return;
        }
        // TODO
        final String oldVariant = prefix + ':' + model.getOriginalName();


        try {
            final Session session = getJCRSession();
            final Node imagesetTemplate = HippoNodeUtils.getNode(session, imageNodePath);
            removeImageVariant(session, imagesetTemplate, oldVariant, model.getName());
            session.save();
            addSuccess("Removed image variant:  " + model.getName(), target);

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            resetSession();
            addError(e.getMessage(), target);
            return;
        }


        images.remove(model);
        target.add(imagesetContainer);

    }

    private void addError(final Serializable message, final AjaxRequestTarget target) {
        feedback.getFeedbackMessagesModel().setObject(new ArrayList<FeedbackMessage>());
        feedback.error(message);
        target.add(feedback);
    }

    private void addSuccess(final Serializable message, final AjaxRequestTarget target) {
        feedback.getFeedbackMessagesModel().setObject(new ArrayList<FeedbackMessage>());
        feedback.success(message);
        target.add(feedback);
    }

    private void removeImageVariant(final Session session, final Node imagesetTemplate, final String variant, final String imageName) throws RepositoryException {
        deleteVariantFromTemplate(session, imagesetTemplate, variant, imageName);
        deleteGalleryProcessorEntryForVariant(session, variant);
    }

    private void setVariantOnTemplate(final Session session, final Node imagesetTemplate, final String prefix, final ImageModel image) throws RepositoryException {
        setTemplateFieldForVariant(session, imagesetTemplate, image.getName());
        setTemplateNodeTypeForVariant(session, imagesetTemplate, image.getName());
        // TODO fix this for hippogallery namespace
        setTemplateTranslationsForVariant(imagesetTemplate, GalleryUtils.getImagesetName(prefix, image.getName()), image.getTranslations());
    }

    private void setTemplateNodeTypeForVariant(final Session session, final Node imagesetTemplate, final String imageName) throws RepositoryException {
        // TODO only required to retrieve node when copy is required
        final Node original = imagesetTemplate.getNode("hipposysedit:nodetype").getNode("hipposysedit:nodetype").getNode("original");
        final String sysPath = original.getParent().getPath() + '/' + imageName;
        final Node copy = HippoNodeUtils.retrieveExistingNodeOrCreateCopy(session, sysPath, original);
        copy.setProperty(HippoNodeUtils.HIPPOSYSEDIT_PATH, prefix + ':' + imageName);
        copy.setProperty(HippoNodeType.HIPPOSYSEDIT_TYPE, HippoGalleryNodeType.IMAGE);
    }

    private void setTemplateFieldForVariant(final Session session, final Node imagesetTemplate, final String imageName) throws RepositoryException {
        // TODO only required to retrieve node when copy is required
        final Node original = imagesetTemplate.getNode("editor:templates").getNode("_default_").getNode("original");
        final String sysPath = original.getParent().getPath() + '/' + imageName;
        final Node copy = HippoNodeUtils.retrieveExistingNodeOrCreateCopy(session, sysPath, original);
        copy.setProperty("caption", imageName);
        copy.setProperty("field", imageName);

    }

    private void deleteVariantFromTemplate(final Session session, final Node imagesetTemplate, final String variant, final String imageName) throws RepositoryException {
        final Node fieldNode = HippoNodeUtils.getNode(session, GalleryUtils.getPathForTemplateVariantField(imagesetTemplate, imageName));
        fieldNode.remove();
        final Node templateNode = HippoNodeUtils.getNode(session, GalleryUtils.getPathForTemplateVariantTemplate(imagesetTemplate, imageName));
        templateNode.remove();
        deleteTranslationsForVariant(imagesetTemplate, variant);
    }

    private void setTemplateTranslationsForVariant(final Node imagesetTemplate, final String property, final List<TranslationModel> translations) throws RepositoryException {
        for (TranslationModel translation : translations) {
            TranslationUtils.setTranslationForNode(imagesetTemplate, property, translation.getLanguage(), translation.getMessage());
        }
    }

    private void createOrUpdateGalleryProcessorEntry(final Session session, final ImageModel imageModel) throws RepositoryException {
        // TODO fix determining variant
        final String variant = prefix + ':' + imageModel.getName();
        final Node node = HippoNodeUtils.retrieveExistingNodeOrCreateCopy(session,
                GalleryUtils.getPathForGalleryProcessorVariant(variant),
                GalleryUtils.getPathForGalleryProcessorVariant(GalleryUtils.HIPPOGALLERY_IMAGE_SET_ORIGINAL));
        node.setProperty("height", Long.valueOf(imageModel.getHeight()));
        node.setProperty("width", Long.valueOf(imageModel.getWidth()));
    }

    private static void deleteGalleryProcessorEntryForVariant(final Session session, final String variant) throws RepositoryException {
        final Node galleryProcessorVariant = GalleryUtils.getGalleryProcessorVariant(session, variant);
        if (galleryProcessorVariant != null) {
            galleryProcessorVariant.remove();
        }
    }

    /**
     * @param imagesetTemplate the imageset template node
     * @param variant          the name of the variant (including prefix e.g. prefix:name)
     * @return
     * @throws RepositoryException
     */
    private static List<TranslationModel> retrieveTranslationsForVariant(final Node imagesetTemplate, final String variant) throws RepositoryException {
        final List<TranslationModel> translations = new ArrayList<>();
        for (Node node : TranslationUtils.getTranslationsFromNode(imagesetTemplate, variant)) {
            translations.add(new TranslationModel(TranslationUtils.getHippoLanguage(node), TranslationUtils.getHippoMessage(node)));
        }
        return translations;
    }

    /**
     * @param imagesetTemplate
     * @param variant          the name of the variant (including prefix e.g. prefix:name)
     * @throws RepositoryException
     */
    private static void deleteTranslationsForVariant(final Node imagesetTemplate, final String variant) throws RepositoryException {
        for (Node node : TranslationUtils.getTranslationsFromNode(imagesetTemplate, variant)) {
            node.remove();
        }
    }


    private final class LoadImageSetButton extends AjaxButton {
        private static final long serialVersionUID = 1L;

        private LoadImageSetButton(String id) {
            super(id);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onLoadImageSetButton(target);
        }
    }


    private final class SubmitButton extends AjaxButton {
        private static final long serialVersionUID = 1L;

        private SubmitButton(String id) {
            super(id);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onFormSubmit(target);
        }
    }

    private final class CopyImageSetButton extends AjaxButton {
        private static final long serialVersionUID = 1L;

        private CopyImageSetButton(String id) {
            super(id);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onCopyImageSetSubmit(target);
        }
    }

    private final class AddButton extends AjaxButton {
        private static final long serialVersionUID = 1L;

        private AddButton(String id) {
            super(id);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onAddButton(target);
        }
    }

    private final class AddImageTranslationButton extends AjaxButton {
        private static final long serialVersionUID = 1L;
        private final ImageModel model;

        private AddImageTranslationButton(final String id, final ImageModel model) {
            super(id);
            this.model = model;
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onAddImageTranslationButton(target, model);
        }
    }

    private final class RemoveImageTranslationButton extends AjaxButton {
        private static final long serialVersionUID = 1L;
        private final ImageModel image;
        private final TranslationModel translation;

        private RemoveImageTranslationButton(final String id, final ImageModel image, final TranslationModel translation) {
            super(id);
            this.image = image;
            this.translation = translation;
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onRemoveImageTranslationButton(target, image, translation);
        }
    }

    private final class SaveButton extends RemoveButton {

        private static final long serialVersionUID = 1L;

        private SaveButton(final String id, final ImageModel model) {
            super(id, model);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            final ImageModel imageModel = getImageModel();
            log.info("saving", imageModel);
            final String imageName = imageModel.getName();
            if (imageModel.isReadOnly()) {
                addError("Read only, cannot save", target);
                return;
            }
            if (Strings.isNullOrEmpty(imageName)) {
                addError("Image name is required", target);
            } else {
                // add nodes:
                final Session session = getJCRSession();
                try {
                    if (Strings.isNullOrEmpty(imageNodePath)) {
                        imageNodePath = GalleryUtils.getNamespacePathForImageset(prefix, name);
                    }

                    if (!session.nodeExists(imageNodePath)) {
                        addError("Node: " + imageNodePath + " not found", target);
                        return;
                    }
                    final Node imagesetTemplate = session.getNode(imageNodePath);

                    // Create/update template fields
                    setVariantOnTemplate(session, imagesetTemplate, prefix, imageModel);

                    // Store translations
/*
                    for (TranslationModel translation : imageModel.getTranslations()) {
                        // TODO fix this for hippogallery namespace
                        TranslationUtils.setTranslationForNode(imagesetTemplate, GalleryUtils.getImagesetName(prefix, imageModel.getName()), translation.getLanguage(), translation.getMessage());
                    }
*/

                    // Create/update gallery processor entry
                    createOrUpdateGalleryProcessorEntry(session, imageModel);

                    if (imageModel.isNameChanged()) {
                        // TODO fix determining variant
                        final String oldVariant = prefix + ':' + imageModel.getOriginalName();
                        removeImageVariant(session, imagesetTemplate, oldVariant, imageModel.getOriginalName());
                    }

                    session.save();
                    addSuccess("Saving image variant:  " + imageModel.getName(), target);
                    //
                } catch (RepositoryException e) {
                    log.error("Error adding variant", e);
                    resetSession();
                    addError(e.getMessage(), target);
                }


            }
            //
        }
    }

    private class RemoveButton extends AjaxButton {
        private static final long serialVersionUID = 1L;
        private final ImageModel model;

        private RemoveButton(String id, ImageModel model) {
            super(id);
            this.model = model;

        }

        public ImageModel getImageModel() {
            return model;
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onRemoveButton(target, model);
        }


    }

    /**
     * A list view of images of an image set.
     */
    private class ImageListView extends ListView<ImageModel> {
        private static final long serialVersionUID = 1L;

        public ImageListView(final String id, final List<ImageModel> images) {
            super(id, images);
        }

        @Override
        protected void populateItem(final ListItem<ImageModel> image) {
            final ImageModel model = image.getModel().getObject();
            final TextField<String> imageName = new TextField<>("imageName", new PropertyModel<String>(model, "name"));
            final TextField<Integer> widthField = new TextField<>("width", new PropertyModel<Integer>(model, "width"));
            final TextField<Integer> heightField = new TextField<>("height", new PropertyModel<Integer>(model, "height"));
            final RemoveButton removeButton = new RemoveButton("remove-imagetype", model);
            final SaveButton saveButton = new SaveButton("save-imagetype", model);
            final AddImageTranslationButton addTranslationButton = new AddImageTranslationButton("add-imagetranslation", model);

            if (model.isReadOnly()) {
                imageName.setEnabled(false);
                widthField.setEnabled(false);
                heightField.setEnabled(false);
                removeButton.setEnabled(false);
                removeButton.setEnabled(false);
                removeButton.setVisible(false);
                saveButton.setVisible(false);

                addTranslationButton.setEnabled(false);
                addTranslationButton.setVisible(false);
            }
            image.add(imageName);
            image.add(widthField);
            image.add(heightField);
            image.add(removeButton);
            image.add(saveButton);
            image.add(addTranslationButton);

            final ImageTranslationsListView translationsView = new ImageTranslationsListView("imageTranslations", model);
            image.add(translationsView);
        }

    }


    /**
     * A list view of the translations of an image.
     */
    private class ImageTranslationsListView extends ListView<TranslationModel> {
        private static final long serialVersionUID = 1L;

        private final ImageModel imageModel;

        /**
         * The constructor for a list view for the translations of an image model.
         *
         * @param id         the wicket id
         * @param imageModel the image model which holds the translations
         */
        public ImageTranslationsListView(final String id, final ImageModel imageModel) {
            super(id, imageModel.getTranslations());
            this.imageModel = imageModel;
        }

        @Override
        protected void populateItem(final ListItem<TranslationModel> translationView) {
            final TranslationModel translationModel = translationView.getModel().getObject();
            final TextField<String> language = new TextField<>("imgTranslationLanguage", new PropertyModel<String>(translationModel, "language"));
            final TextField<String> message = new TextField<>("imgTranslationMessage", new PropertyModel<String>(translationModel, "message"));
            final RemoveImageTranslationButton removeTranslationButton = new RemoveImageTranslationButton("remove-image-translation", imageModel, translationModel);

            if (imageModel.isReadOnly()) {
                translationView.setEnabled(false);
                translationView.setEnabled(false);
                removeTranslationButton.setEnabled(false);
                removeTranslationButton.setVisible(false);
            }
            translationView.add(language);
            translationView.add(message);
            translationView.add(removeTranslationButton);
        }
    }

}
