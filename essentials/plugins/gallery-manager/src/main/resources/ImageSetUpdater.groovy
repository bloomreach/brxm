/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.commons.io.IOUtils
import org.apache.jackrabbit.JcrConstants
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor
import org.hippoecm.repository.api.HippoNodeType
import org.hippoecm.repository.gallery.HippoGalleryNodeType
import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.Node
import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.query.Query
import javax.jcr.query.QueryManager
import javax.jcr.query.QueryResult

/**
 * Groovy script to update instances of image sets, reapplying the configuration from the gallery processor.
 * Per default, existing variants are overwritten and default thumbnails are skipped. This behavior is configurable.
 *
 * XPath query: content/gallery//element(*, hippogallery:imageset)
 *
 * Parameters: { "overwrite": true,
 *               "skipDefaultThumbnail" : true }
 */
class ImageSetUpdater extends BaseNodeUpdateVisitor {

    private static final String HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE = "hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    protected static final String CONFIG_PARAM_WIDTH = "width";
    protected static final String CONFIG_PARAM_HEIGHT = "height";
    protected static final String CONFIG_PARAM_UPSCALING = "upscaling";
    protected static final String CONFIG_PARAM_OPTIMIZE = "optimize";
    protected static final String CONFIG_PARAM_COMPRESSION = "compression";

    protected static final Long DEFAULT_WIDTH = 0L;
    protected static final Long DEFAULT_HEIGHT = 0L;
    protected static final boolean DEFAULT_UPSCALING = false;
    protected static final String DEFAULT_OPTIMIZE = "quality";
    protected static final double DEFAULT_COMPRESSION = 1.0;

    // SCALING_STRATEGY_MAP copied from org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessorPlugin
    private static final Map<String, ImageUtils.ScalingStrategy> SCALING_STRATEGY_MAP = new LinkedHashMap<>();
    static {
        SCALING_STRATEGY_MAP.put("auto", ImageUtils.ScalingStrategy.AUTO);
        SCALING_STRATEGY_MAP.put("speed", ImageUtils.ScalingStrategy.SPEED);
        SCALING_STRATEGY_MAP.put("speed.and.quality", ImageUtils.ScalingStrategy.SPEED_AND_QUALITY);
        SCALING_STRATEGY_MAP.put("quality", ImageUtils.ScalingStrategy.QUALITY);
        SCALING_STRATEGY_MAP.put("best.quality", ImageUtils.ScalingStrategy.BEST_QUALITY);
    }

    private final Map<String, ScalingParameters> imageVariantParameters = new HashMap<String, ScalingParameters>();
    private final Map<String, List<String>> imageSetVariants = new HashMap<String, List<String>>();

    private boolean overwrite = true;
    private boolean skipDefaultThumbnail = true;

    public void initialize(Session session) throws RepositoryException {
        try {
            if (this.parametersMap["overwrite"] != null) {
                overwrite = parametersMap["overwrite"]
            }
            if (parametersMap["skipDefaultThumbnail"] != null) {
                skipDefaultThumbnail = parametersMap["skipDefaultThumbnail"]
            }

            Node configNode = session.getRootNode().getNode(HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE);
            getImageVariantParametersFromProcessor(configNode);
            getImageSetVariantsFromNamespace(session);
        } catch (RepositoryException e) {
            log.error("Exception while retrieving image set variants configuration", e);
        }

        log.info "Initialized script ${this.getClass().getName()} with parameters: overwrite=${overwrite}, skipDefaultThumbnail=${skipDefaultThumbnail}"
    }


    boolean doUpdate(Node node) {
        try {
            processImageSet(node);
            return true;
        } catch (RepositoryException e) {
            log.error("Failed in generating image variants", e);
            node.getSession().refresh(false/*keepChanges*/)
        }
        return false;
    }

    @Override
    boolean undoUpdate(final Node node) throws RepositoryException, UnsupportedOperationException {
        return false
    }

    private void processImageSet(Node node) throws RepositoryException {

        final List<String> imageSetVariants = imageSetVariants.get(node.getPrimaryNodeType().getName());
        if (imageSetVariants == null) {
            log.warn("Could not find image set {}, skipping processing node {}", node.getPrimaryNodeType().getName(), node.getPath());
            return
        }
        if (imageSetVariants.isEmpty()) {
            log.warn("Image set {} has no variants to regenerate, skipping processing node {}", node.getPrimaryNodeType().getName(), node.getPath());
            return
        }

        Node data;
        if (node.hasNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL)) {
            data = node.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
        } else {
            // hippogallery:thumbnail is the only mandatory image variant in hippogallery.cnd (not hippogalley:original!)
            data = node.getNode(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL)
        }

        for (String variantName : imageSetVariants) {
            processImageVariant(node, data, variantName);
        }
    }

    private void processImageVariant(Node node, Node data, String variantName) throws RepositoryException {

        // original not to be reconfigured/regenerated so skip it
        if (HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(variantName)) {
            log.debug "Skipping processing the original"
            return
        }

        // thumbnail can be reconfigured, then only regenerate by parameter
        if (HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName) && skipDefaultThumbnail) {
           log.debug "Parameter skipDefaultThumbnail=true: skipping processing the default thumbnail variant"
           return
        }

        final ScalingParameters parameters = imageVariantParameters.get(variantName);
        if (parameters == null) {
            log.warn("No parameters found for image variant {}. Skipping variant for node {}", variantName, node.path);
            return;
        }
        if (parameters.width == 0 && parameters.height == 0) {
            log.warn("No width and height available for image variant {}. Skipping variant for node {}", variantName, node.path);
            return;
        }

        Node variant;
        if (node.hasNode(variantName)) {
            if (!overwrite) {
                log.info("Parameter overwrite=false: skipping existing variant {} of node {}", variantName, node.path);
                return;
            }
            variant = node.getNode(variantName);
        } else {
            variant = node.addNode(variantName, HippoGalleryNodeType.IMAGE);
        }

        createImageVariant(node, data, variant, parameters);
    }

    private void createImageVariant(Node node, Node data, Node variant, ScalingParameters parameters) throws RepositoryException {

        InputStream dataInputStream = null;

        try {
            if (!data.hasProperty(JcrConstants.JCR_DATA)) {
                log.warn("Image variant {} for node {} does not have {} property. Variant not updated.",
                        variant.getName(), node.getPath(), JcrConstants.JCR_DATA)
                return
            }

            dataInputStream = data.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
            String mimeType = data.getProperty(JcrConstants.JCR_MIMETYPE).getString();

            ScalingGalleryProcessor scalingGalleryProcessor = new ScalingGalleryProcessor();

            scalingGalleryProcessor.addScalingParameters(variant.getName(), parameters);
            scalingGalleryProcessor.initGalleryResource(variant, dataInputStream, mimeType, "", Calendar.getInstance());

            log.info("Image variant {} (re)generated for node {}", variant.getName(), node.getPath());
        } finally {
            IOUtils.closeQuietly(dataInputStream);
        }
    }

    private void getImageSetVariantsFromNamespace(Session session) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("hippo:namespaces//element(*, hippogallery:imageset)", "xpath");
        QueryResult queryResult = query.execute();
        NodeIterator nodeIterator = queryResult.getNodes();

        // looking up fields of type hippogallery:image in the nodetype of a definition
        while (nodeIterator.hasNext()) {
            Node prototype = nodeIterator.nextNode();

            Node doctype = prototype.getParent().getParent();
            Node nodetype;
            String relNodeTypePath = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;
            if (doctype.hasNode(relNodeTypePath)) {
                nodetype = doctype.getNode(relNodeTypePath);
            }
            else {
                log.warn "No node ${relNodeTypePath} found below node ${prototype.path}"
                continue
            }

            NodeIterator fields = nodetype.getNodes();

            List<String> imageVariants = new ArrayList<String>();

            while (fields.hasNext()) {
                Node field = fields.nextNode();

                if (field.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE) &&
                    HippoGalleryNodeType.IMAGE.equals(field.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString())) {

                    // read variant name from configuration, or construct namespace:field
                    String variantName = (field.hasProperty(HippoNodeType.HIPPO_PATH)) ?
                                            field.getProperty(HippoNodeType.HIPPO_PATH).getString() :
                                            doctype.getParent().getName() + ":" + field.getName();

                    // original not to be reconfigured/regenerated so skip it
                    if (HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(variantName)) {
                        log.debug "Skipping reading original variant from '${prototype.getPrimaryNodeType().getName()}' namespace"
                        continue
                    }

                    // thumbnail can be reconfigured, then only regenerate by parameter
                    if (HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName) && skipDefaultThumbnail) {
                        log.debug "Parameter skipDefaultThumbnail=true: skipping reading default thumbnail variant from '${prototype.getPrimaryNodeType().getName()}' namespace"
                        continue
                    }

                    imageVariants.add(variantName);
                }
            }
            imageSetVariants.put(prototype.getPrimaryNodeType().getName(), imageVariants);
            log.info "Read image set '${prototype.getPrimaryNodeType().getName()}' from namespace with fields '${imageVariants}'"
        }
    }

    private void getImageVariantParametersFromProcessor(Node configNode) throws RepositoryException {
        NodeIterator variantNodes = configNode.getNodes();

        while (variantNodes.hasNext()) {
            Node variantNode = variantNodes.nextNode();
            String variantName = variantNode.getName();

            // original not to be reconfigured/regenerated so skip it
            if (HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(variantName)) {
                log.debug "Skipping reading original variant configuration"
                continue
            }

            // thumbnail can be reconfigured, then only regenerate by parameter
            if (HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName) && skipDefaultThumbnail) {
                log.debug "Parameter skipDefaultThumbnail=true: skipping reading default thumbnail variant configuration"
                continue
            }

            int width = variantNode.hasProperty(CONFIG_PARAM_WIDTH) ? variantNode.getProperty(CONFIG_PARAM_WIDTH).getLong() : DEFAULT_WIDTH;
            int height = variantNode.hasProperty(CONFIG_PARAM_HEIGHT) ? variantNode.getProperty(CONFIG_PARAM_HEIGHT).getLong() : DEFAULT_HEIGHT;
            boolean upscaling = variantNode.hasProperty(CONFIG_PARAM_UPSCALING) ?
                    variantNode.getProperty(CONFIG_PARAM_UPSCALING).boolean : DEFAULT_UPSCALING
            String optimize = variantNode.hasProperty(CONFIG_PARAM_OPTIMIZE) ?
                    variantNode.getProperty(CONFIG_PARAM_OPTIMIZE).string : DEFAULT_OPTIMIZE
            float compression = variantNode.hasProperty(CONFIG_PARAM_COMPRESSION) ?
                    variantNode.getProperty(CONFIG_PARAM_COMPRESSION).double : DEFAULT_COMPRESSION


            ImageUtils.ScalingStrategy strategy = SCALING_STRATEGY_MAP.get(optimize);
            if (strategy == null) {
                log.warn "Image variant '${variantName}' specifies an unknown scaling optimization strategy " +
                        "'${optimize}'. Possible values are ${SCALING_STRATEGY_MAP.keySet()}. Falling back to" +
                        " '${DEFAULT_OPTIMIZE}' instead."
                strategy = SCALING_STRATEGY_MAP.get(DEFAULT_OPTIMIZE);
            }

            ScalingParameters parameters = new ScalingParameters(width.intValue(), height.intValue(), upscaling, strategy, compression)
            log.info "Read image set variant '${variantName}' from processor with scalingParameters '${parameters}'"

            imageVariantParameters.put(variantName, parameters);
        }
    }
}
