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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
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
    private static Logger log = LoggerFactory.getLogger(GalleryPluginResource.class);


    /**
     * Creates new image set
     */
    @POST
    @Path("/create")
    public MessageRestful createImageSet(final PostPayloadRestful payload, @Context ServletContext servletContext) {
        final Map<String, String> values = payload.getValues();
        return createImageSet(getContext(servletContext), values.get("imageSetPrefix"), values.get("imageSetName"));
    }

    /**
     * Adds image variant to the existing image set
     */
    @POST
    @Path("/addvariant")
    public MessageRestful addVariant(final PostPayloadRestful payload, @Context ServletContext servletContext) {
        final Map<String, String> values = payload.getValues();
        /*
        *
        *    var payload = Essentials.addPayloadData("imageVariantName", $scope.imageVariantName, null);
            Essentials.addPayloadData("selectedImageSet", $scope.selectedImageSet.name, payload);*/
        final String imageVariantName = values.get("imageVariantName");
        final String selectedImageSet = values.get("selectedImageSet");
        if (Strings.isNullOrEmpty(imageVariantName) || Strings.isNullOrEmpty(selectedImageSet)) {
            return new ErrorMessageRestful("Image set name or image variant name were empty");
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
            return new ErrorMessageRestful("Couldn't load imageset model for: " + selectedImageSet);
        }

        return new MessageRestful("test");
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


    private List<ImageModel> populateTypes(final Session session, final Node imagesetTemplate) throws RepositoryException {
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
                model.setParentPath(variant.getParent().getPath());

                // Get values from gallery processor variant
                final Node processorVariant = GalleryUtils.getGalleryProcessorVariant(session, model.getType());
                if (processorVariant != null) {
                    model.setHeight(HippoNodeUtils.getLongProperty(processorVariant, "height", 0L).intValue());
                    model.setWidth(HippoNodeUtils.getLongProperty(processorVariant, "width", 0L).intValue());
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
            return populateTypes(session, HippoNodeUtils.getNode(session, imageNodePath));

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            GlobalUtils.refreshSession(session, false);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return new ArrayList<>();
    }


    private MessageRestful createImageSet(final PluginContext context, final String prefix, final String name) {
        if (Strings.isNullOrEmpty(prefix) || Strings.isNullOrEmpty(name)) {
            return new ErrorMessageRestful("Imageset namespace prefix or it's name were empty");
        }
        final Session session = context.createSession();
        final String nodeType = prefix + ':' + name;
        try {

            final String uri = GalleryUtils.getGalleryURI(prefix);
            // Check whether node type already exists
            if (CndUtils.nodeTypeExists(context, nodeType)) {
                if (CndUtils.isNodeOfSuperType(context, nodeType, HippoGalleryNodeType.IMAGE_SET)) {

                    return new ErrorMessageRestful("ImageSet already exists: " + nodeType);
                } else {
                    // Node type exists and is no image set
                    return new ErrorMessageRestful("Node of type already exists: " + nodeType + " but it is not an ImageSet type");
                }
            }
            if (CndUtils.namespacePrefixExists(context, prefix)) {
                // No need to register namespace because it already exists
                log.debug("Already registered uri {}", uri);
            } else if (CndUtils.namespaceUriExists(context, uri)) {
                // Unable to register namespace for already existing URI
                log.error("Namespace URI '{}' already exists", uri);
                return new ErrorMessageRestful("Namespace URI " + uri + " already exists");
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
            final Node imageNode = GalleryUtils.createImagesetNamespace(session, prefix, name, "/hippo:namespaces/hippogallery/imageset");
            session.save();
            log.debug("Created node: {}", imageNode.getPath());

        } catch (RepositoryException e) {
            log.error("Error creating image set", e);
            return new ErrorMessageRestful("Error creating image set: " + e.getMessage());
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return new MessageRestful("Successfully created imageset: " + nodeType);
    }


    private void createOrLoadImageSet(final String prefix, final String name, final String imageNodePathToCopy, final PluginContext context) {
        final Session session = context.createSession();
        try {

            final String nodeType = prefix + ':' + name;
            final String uri = GalleryUtils.getGalleryURI(prefix);
            // Check whether node type already exists
            if (CndUtils.nodeTypeExists(context, nodeType)) {
                if (CndUtils.isNodeOfSuperType(context, nodeType, HippoGalleryNodeType.IMAGE_SET)) {
                    // Node type exists and is and image set which can be loaded
                    loadImageSet(prefix, name, context);
                    return;
                } else {
                    // Node type exists and is no image set
                    return;
                }
            }

            // Check whether namespace needs to / can be created
            if (CndUtils.namespacePrefixExists(context, prefix)) {
                // No need to register namespace because it already exists
                log.debug("Already registered uri {}", uri);
            } else if (CndUtils.namespaceUriExists(context, uri)) {
                // Unable to register namespace for already existing URI
                log.error("Namespace URI '{}' already exists", uri);
                return;
            } else {
                // Register new namespace
                CndUtils.registerNamespace(context, prefix, uri);
            }

            CndUtils.createHippoNamespace(context, prefix);
            CndUtils.registerDocumentType(context, prefix, name, false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);

            // copy node:
            final Node imageNode;
            if (imageNodePathToCopy != null) {
                // Copy a previously used image variant
                // TODO imageNodePath
                final String imageNodePath = "";
                imageNode = GalleryUtils.createImagesetNamespace(session, prefix, name, imageNodePath);
            } else {
                // Copy the default original image variant
                imageNode = GalleryUtils.createImagesetNamespace(session, prefix, name);
            }


            populateTypes(session, imageNode);

            session.save();

            // Select the created imageset in the dropdown
            String selectedImageSet = prefix + ':' + name;

        } catch (RepositoryException e) {
            log.error("Error in gallery plugin", e);
            GlobalUtils.refreshSession(session, false);
        }
        GlobalUtils.cleanupSession(session);
    }


}
