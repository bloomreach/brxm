/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.gallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.dashboard.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GalleryUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/galleryplugin")
public class GalleryPluginResource extends BaseResource {


    private static final Pattern NS_PATTERN = Pattern.compile(":");
    public static final String HIPPO_TRANSLATION = "hippo:translation";
    public static final String HIPPO_PROPERTY = "hippo:property";
    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";
    public static final String HIPPOSYSEDIT_PROTOTYPE = "hipposysedit:prototype";
    private static Logger log = LoggerFactory.getLogger(GalleryPluginResource.class);
    public static final String PROCESSOR_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    /**
     * Updates an image model
     */
    @POST
    @Path("/update")
    public MessageRestful update(final ImageModel payload, @Context ServletContext servletContext, @Context HttpServletResponse response) {
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        try {
            final String myType = payload.getType();
            final Node namespaceNode = session.getNode("/hippo:namespaces/" + GlobalUtils.getNamespacePrefix(payload.getParentNamespace()) + '/' + GlobalUtils.getNameAfterPrefix(payload.getParentNamespace()));
            // add translations...
            processTranslations(payload, myType, namespaceNode, false);
            // add processor stuff...
            updateProcessorNode(payload, session, myType);
            // update sets
            scheduleImageScript(context);
            session.save();
            return new MessageRestful("Successfully updated image variant: " + myType);

        } catch (RepositoryException e) {
            log.error("Error saving image path", e);
            return createErrorMessage("Failed to update image variant: " + e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
    }


    /**
     * Removes image variant
     */
    @POST
    @Path("/remove")
    public MessageRestful remove(final ImageModel payload, @Context ServletContext servletContext, @Context HttpServletResponse response) {
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        try {
            final String myType = payload.getType();
            if (payload.isReadOnly()) {
                return createErrorMessage("Cannot remove read only node: " + myType, response);
            }
            final Node namespaceNode = session.getNode("/hippo:namespaces/" + GlobalUtils.getNamespacePrefix(payload.getParentNamespace()) + '/' + GlobalUtils.getNameAfterPrefix(payload.getParentNamespace()));
            // remove translations...
            processTranslations(payload, myType, namespaceNode, true);
            // remove template, prototype and node type:
            final Node nodeTypeNode = namespaceNode.getNode(HIPPOSYSEDIT_NODETYPE).getNode(HIPPOSYSEDIT_NODETYPE);
            final String name = payload.getName();
            if (nodeTypeNode.hasNode(name)) {
                nodeTypeNode.getNode(name).remove();
            }
            final Node prototypeNode = namespaceNode.getNode(HIPPOSYSEDIT_PROTOTYPE + 's').getNode(HIPPOSYSEDIT_PROTOTYPE);
            if (prototypeNode.hasNode(myType)) {
                prototypeNode.getNode(myType).remove();
            }
            final Node editorNode = namespaceNode.getNode("editor:templates").getNode("_default_");
            if (editorNode.hasNode(name)) {
                editorNode.getNode(name).remove();
            }
            // remove processing node
            final Node processorNode = session.getNode(PROCESSOR_PATH);
            if (processorNode.hasNode(myType)) {
                processorNode.getNode(myType).remove();
            }
            session.save();
            return new MessageRestful("Successfully removed image variant: " + myType);

        } catch (RepositoryException e) {
            log.error("Error removing image variant", e);
            return createErrorMessage("Failed to remove image variant: " + e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
    }


    /**
     * Creates new image set
     */
    @POST
    @Path("/create")
    public MessageRestful create(final PostPayloadRestful payload, @Context ServletContext servletContext, @Context HttpServletResponse response) {
        final Map<String, String> values = payload.getValues();
        final String imageSetName = values.get("imageSetName");
        final boolean updateExisting = Boolean.valueOf(values.get("updateExisting"));
        final String imageSetPrefix = values.get("imageSetPrefix");
        final MessageRestful imageSet = createImageSet(getContext(servletContext), imageSetPrefix, imageSetName, response);
        if (updateExisting && imageSet.isSuccessMessage()) {
            final PluginContext context = getContext(servletContext);
            final Session session = context.createSession();
            try {
                final String rootDestination = "/hippo:configuration/hippo:queries/hippo:templates";
                final String newImageNamespace = imageSetPrefix + ':' + imageSetName;
                // image
                final String queryMiddleName = imageSetPrefix + '-' + imageSetName;
                final String imageName = "new-" + queryMiddleName + "-image";
                final Node imageNode = JcrUtils.copy(session, rootDestination + "/new-image", rootDestination + '/' + imageName);
                final String oldImageQuery = imageNode.getProperty("jcr:statement").getString();
                final String newImageQuery = oldImageQuery.replaceAll("new\\-image", imageName);
                imageNode.setProperty("jcr:statement", newImageQuery);

                //..    imageNode.setProperty("new-image/hippostd:templates/image/image", newImageNamespace);
                // folder
                final String folderName = "new-" + queryMiddleName + "-folder";
                final Node folderNode = JcrUtils.copy(session, rootDestination + "/new-image-folder", rootDestination + '/' + folderName);
                final String oldFolderQuery = folderNode.getProperty("jcr:statement").getString();
                final String newFolderQuery = oldFolderQuery.replaceAll("new\\-image\\-folder", folderName);
                folderNode.setProperty("jcr:statement", newFolderQuery);
                final Node imageGalleryNode = folderNode.getNode("hippostd:templates").getNode("image gallery");
                imageGalleryNode.setProperty("hippostd:foldertype", new String[]{folderName});
                imageGalleryNode.setProperty("hippostd:gallerytype", new String[]{newImageNamespace});
                // update existing folders:
                final Query query = session.getWorkspace().getQueryManager().createQuery("//content//element(*, hippogallery:stdImageGallery)", "xpath");
                final NodeIterator nodes = query.execute().getNodes();
                while (nodes.hasNext()) {
                    final Node imageFolderNode = nodes.nextNode();
                    imageFolderNode.setProperty("hippostd:foldertype", new String[]{folderName});
                    imageFolderNode.setProperty("hippostd:gallerytype", new String[]{newImageNamespace});
                }
                // change primary types:
                final Query handleQuery = session.getWorkspace().getQueryManager().createQuery("//content/gallery//element(*, hippo:handle)", "xpath");
                final NodeIterator handleNodes = handleQuery.execute().getNodes();
                while (handleNodes.hasNext()) {
                    final Node handle = handleNodes.nextNode();
                    final String name = handle.getName();
                    if (handle.hasNode(name)) {
                        final Node myImageNode = handle.getNode(name);
                        myImageNode.setPrimaryType(newImageNamespace);
                    }else{
                        log.warn("handle {}", handle.getPath());
                    }
                }
                session.save();
            } catch (RepositoryException e) {
                log.error("Error creating query nodes", e);
            } finally {
                GlobalUtils.cleanupSession(session);
            }

        }
        return imageSet;
    }

    /**
     * Adds image variant to the existing image set
     */
    @POST
    @Path("/addvariant")
    public MessageRestful addVariant(final PostPayloadRestful payload, @Context ServletContext servletContext, @Context HttpServletResponse response) {
        final Map<String, String> values = payload.getValues();
        final String imageVariantName = values.get("imageVariantName");
        final String selectedImageSet = values.get("selectedImageSet");
        if (Strings.isNullOrEmpty(imageVariantName) || Strings.isNullOrEmpty(selectedImageSet)) {
            return createErrorMessage("Image set name or image variant name were empty", response);
        }
        GalleryModel ourModel = null;
        final List<GalleryModel> models = fetchExisting(servletContext);
        for (GalleryModel model : models) {
            if (model.getName().equals(selectedImageSet)) {
                ourModel = model;
                break;
            }
        }
        if (ourModel == null) {
            return createErrorMessage("Couldn't load imageset model for: " + selectedImageSet, response);
        }


        final ImageModel imageModel = extractBestModel(ourModel);
        final PluginContext context = getContext(servletContext);
        final boolean created = GalleryUtils.createImagesetVariant(context, ourModel.getPrefix(), ourModel.getNameAfterPrefix(), imageVariantName, imageModel.getName());
        if (created) {
            // add processor node:
            final Session session = context.createSession();
            try {
                createProcessingNode(session, ourModel.getPrefix() + ':' + imageVariantName);
                session.save();
            } catch (RepositoryException e) {
                log.error("Error creating processing node", e);
            } finally {

                GlobalUtils.cleanupSession(session);
            }
            scheduleImageScript(context);


            return new MessageRestful("Image variant:  " + imageVariantName + " successfully created");
        }
        return createErrorMessage("Failed to create image variant: " + imageVariantName, response);

    }

    public void scheduleImageScript(final PluginContext context) {
        // schedule updater script  so new variants are created:
        final XmlInstruction instruction = new XmlInstruction();
        instruction.setAction(PluginInstruction.COPY);
        instruction.setSource("image_set_updater.xml");
        instruction.setTarget("/hippo:configuration/hippo:update/hippo:queue");
        final InstructionExecutor executor = new PluginInstructionExecutor();
        final InstructionSet instructionSet = new PluginInstructionSet();
        instructionSet.addInstruction(instruction);
        getInjector().autowireBean(instruction);
        getInjector().autowireBean(executor);
        executor.execute(instructionSet, context);
    }

    public ImageModel extractBestModel(final GalleryModel ourModel) {
        final List<ImageModel> models = ourModel.getModels();
        ImageModel bestModel = null;
        for (ImageModel model : models) {
            if (model.getName().equals("original")) {
                return model;
            }
            if (model.getName().equals("thumbnail")) {
                bestModel = model;
            }
            if (bestModel == null) {
                bestModel = model;
            }

        }
        return null;
    }

    /**
     * Fetch existing gallery namespaces
     */
    @GET
    @Path("/")
    public List<GalleryModel> fetchExisting(@Context ServletContext servletContext) {

        final List<GalleryModel> models = new ArrayList<>();
        try {
            final List<String> existingImageSets = CndUtils.getNodeTypesOfType(getContext(servletContext), HippoGalleryNodeType.IMAGE_SET, true);
            final PluginContext context = getContext(servletContext);

            for (String existingImageSet : existingImageSets) {
                final String[] ns = NS_PATTERN.split(existingImageSet);
                final List<ImageModel> imageModels = loadImageSet(ns[0], ns[1], context);
                final GalleryModel galleryModel = new GalleryModel(existingImageSet);
                if (existingImageSet.equals(HippoGalleryNodeType.IMAGE_SET)) {
                    galleryModel.setReadOnly(true);
                    // mm: for time being, we'll not return internal imageset
                    continue;
                }
                galleryModel.setModels(imageModels);
                // retrieve parent path: we *should* always have original and thumbnail:
                if (imageModels.size() > 0) {
                    galleryModel.setPath(imageModels.get(0).getParentPath());
                }
                models.add(galleryModel);
            }
        } catch (RepositoryException e) {
            log.error("Error fetching image types ", e);
        }

        return models;

    }


    //############################################
    // UTILS
    //############################################


    private void updateProcessorNode(final ImageModel payload, final Session session, final String myType) throws RepositoryException {
        final Node processingNode = createProcessingNode(session, myType);
        processingNode.setProperty("height", payload.getHeight());
        processingNode.setProperty("width", payload.getWidth());
        processingNode.setProperty("upscaling", payload.isUpscaling());
        processingNode.setProperty("optimize", payload.getOptimize());
        processingNode.setProperty("compression", payload.getCompression());
    }

    private void processTranslations(final ImageModel payload, final String myType, final Node namespaceNode, final boolean justRemove) throws RepositoryException {
        final NodeIterator nodes = namespaceNode.getNodes();
        while (nodes.hasNext()) {
            final Node aNode = nodes.nextNode();
            final String propName = HippoNodeUtils.getStringProperty(aNode, HIPPO_PROPERTY);
            if (aNode.getName().equals(HIPPO_TRANSLATION)
                    && propName != null
                    && propName.equals(myType)) {
                // remove
                aNode.remove();
            }
        }
        if (justRemove) {
            return;
        }
        // add new translations
        final List<TranslationModel> translations = payload.getTranslations();
        for (TranslationModel trans : translations) {
            if (Strings.isNullOrEmpty(trans.getLanguage())) {
                log.debug("Skipping empty language for translation: {}", trans);
                continue;
            }
            final Node node = namespaceNode.addNode(HIPPO_TRANSLATION, HIPPO_TRANSLATION);
            node.setProperty("hippo:language", trans.getLanguage());
            node.setProperty("hippo:message", trans.getMessage());
            node.setProperty(HIPPO_PROPERTY, myType);
        }
    }


    private List<ImageModel> populateTypes(final Session session, final Node imagesetTemplate, final String parentNs) throws RepositoryException {
        final List<ImageModel> images = new ArrayList<>();
        if (imagesetTemplate == null) {
            return images;
        }
        for (final Node variant : GalleryUtils.getFieldVariantsFromTemplate(imagesetTemplate)) {
            final String prefix = HippoNodeUtils.getPrefixFromType(HippoNodeUtils.getStringProperty(variant, HippoNodeUtils.HIPPOSYSEDIT_PATH));
            if (prefix != null) {
                final ImageModel model = new ImageModel(prefix);
                model.setName(variant.getName());
                model.setPath(variant.getPath());
                model.setParentNamespace(parentNs);
                model.setParentPath(variant.getParent().getPath());

                // Get values from gallery processor variant
                final Node processorVariant = GalleryUtils.getGalleryProcessorVariant(session, model.getType());
                if (processorVariant != null) {
                    model.setHeight(HippoNodeUtils.getLongProperty(processorVariant, "height", 0L).intValue());
                    model.setWidth(HippoNodeUtils.getLongProperty(processorVariant, "width", 0L).intValue());
                    model.setUpscaling(HippoNodeUtils.getBooleanProperty(processorVariant, "upscaling"));
                    model.setOptimize(HippoNodeUtils.getStringProperty(processorVariant, "optimize", "quality"));
                    model.setCompression(HippoNodeUtils.getDoubleProperty(processorVariant, "compression", 1D));
                }

                // Retrieve and set the translations to the model
                model.setTranslations(retrieveTranslationsForVariant(imagesetTemplate, model.getType()));
                images.add(model);
            }
        }
        return images;
    }

    /**
     * @param imagesetTemplate the imageset template node
     * @param variant          the name of the variant (including prefix e.g. prefix:name)
     * @return
     * @throws javax.jcr.RepositoryException
     */
    private static List<TranslationModel> retrieveTranslationsForVariant(final Node imagesetTemplate, final String variant) throws RepositoryException {
        final List<TranslationModel> translations = new ArrayList<>();
        for (Node node : TranslationUtils.getTranslationsFromNode(imagesetTemplate, variant)) {
            translations.add(new TranslationModel(TranslationUtils.getHippoLanguage(node), TranslationUtils.getHippoMessage(node), TranslationUtils.getHippoProperty(node)));
        }
        return translations;
    }


    /**
     * Load an image set.
     *
     * @param prefix the imageset type prefix
     * @param name   the imageset type name
     */
    private List<ImageModel> loadImageSet(final String prefix, final String name, final PluginContext context) {
        final Session session = context.createSession();
        try {

            String imageNodePath = GalleryUtils.getNamespacePathForImageset(prefix, name);
            return populateTypes(session, HippoNodeUtils.getNode(session, imageNodePath), prefix + ':' + name);

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            GlobalUtils.refreshSession(session, false);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return new ArrayList<>();
    }


    private MessageRestful createImageSet(final PluginContext context, final String prefix, final String name, final HttpServletResponse response) {
        if (Strings.isNullOrEmpty(prefix) || Strings.isNullOrEmpty(name)) {
            return createErrorMessage("Error message", response);

        }
        final Session session = context.createSession();
        final String nodeType = prefix + ':' + name;
        try {

            final String uri = GalleryUtils.getGalleryURI(context, prefix);
            // Check whether node type already exists
            if (CndUtils.nodeTypeExists(context, nodeType)) {
                if (CndUtils.isNodeOfSuperType(context, nodeType, HippoGalleryNodeType.IMAGE_SET)) {

                    return createErrorMessage("ImageSet already exists: " + nodeType, response);
                } else {
                    // Node type exists and is no image set
                    return createErrorMessage("Node of type already exists: " + nodeType + " but it is not an ImageSet type", response);
                }
            }
            if (CndUtils.namespacePrefixExists(context, prefix)) {
                // No need to register namespace because it already exists
                log.debug("Already registered uri {}", uri);
            } else if (CndUtils.namespaceUriExists(context, uri)) {
                // Unable to register namespace for already existing URI
                log.error("Namespace URI '{}' already exists", uri);
                return createErrorMessage("Namespace URI " + uri + " already exists", response);
            } else {
                // Register new namespace
                CndUtils.registerNamespace(context, prefix, uri);
                GlobalUtils.refreshSession(session, false);
            }

            CndUtils.createHippoNamespace(context, prefix);
            GlobalUtils.refreshSession(session, false);
            CndUtils.registerDocumentType(context, prefix, name, false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
            // copy node:
            GlobalUtils.refreshSession(session, false);
            final Node imageNode = GalleryUtils.createImagesetNamespace(context, session, prefix, name, "/hippo:namespaces/hippogallery/imageset");
            session.save();
            log.debug("Created node: {}", imageNode.getPath());

        } catch (RepositoryException e) {
            log.error("Error creating image set", e);
            return createErrorMessage("Error creating image set: " + e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return new MessageRestful("Successfully created imageset: " + nodeType);
    }

    private Node createProcessingNode(final Session session, final String nodeType) throws RepositoryException {
        final Node processorNode = session.getNode(PROCESSOR_PATH);
        if (processorNode.hasNode(nodeType)) {
            log.debug("Processing node: {}, already exists", nodeType);
            return processorNode.getNode(nodeType);
        }
        final Node myProcessor = processorNode.addNode(nodeType, "frontend:pluginconfig");
        myProcessor.setProperty("height", 0L);
        myProcessor.setProperty("width", 0L);
        myProcessor.setProperty("upscaling", false);
        return processorNode;
    }


}
