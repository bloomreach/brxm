package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.hippoecm.repository.gallery.HippoGalleryNodeType;

/**
 * @version "$Id: GalleryUtils.java 163480 2013-05-06 09:19:16Z mmilicevic $"
 */
public final class GalleryUtils {

    private static Logger log = LoggerFactory.getLogger(GalleryUtils.class);

    public static final String HIPPOGALLERY_IMAGE = "hippogallery:image";
    public static final String HIPPOGALLERY_IMAGE_WIDTH = "hippogallery:width";
    public static final String HIPPOGALLERY_IMAGE_HEIGHT = "hippogallery:height";
    public static final String HIPPOGALLERY_IMAGE_SET = "hippogallery:imageset";
    public static final String HIPPOGALLERY_IMAGE_SET_FILE_NAME = "hippogallery:filename";
    public static final String HIPPOGALLERY_IMAGE_SET_DESCRIPTION = "hippogallery:description";
    public static final String HIPPOGALLERY_IMAGE_SET_THUMBNAIL = "hippogallery:thumbnail";
    public static final String HIPPOGALLERY_IMAGE_SET_ORIGINAL = "hippogallery:original";
    public static final String HIPPOGALLERY_RELAXED = "hippogallery:relaxed";

    public static final String GALLERY_PROCESSOR_SERVICE_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    private static final String DEFAULT_IMAGESET_BLUEPRINT = "/hippo:namespaces/hippogallery/imageset";


    public static String getNamespacePathForImageset(final String prefix, final String name) {
        final StringBuilder sb = new StringBuilder();
        sb.append('/');
        sb.append(HippoNodeType.NAMESPACES_PATH);
        sb.append('/');
        sb.append(prefix);
        sb.append('/');
        sb.append(name);
        return sb.toString();
    }

    /**
     * Return the imageset URI belonging based on a prefix.
     *
     * @param prefix the imageset prefix
     * @return the uri
     */
    public static String getGalleryURI(final String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return null;
        }
        return "http://www.onehippo.org/gallery/" + prefix + "/nt/2.0";
    }

    /**
     * Create a new imageset namespace node for the imageset with the given prefix and name. The new imageset node will
     * be based on a copy of the template under {@code DEFAULT_IMAGESET_BLUEPRINT}.
     *
     * @param session the JCR session
     * @param prefix  the imageset prefix
     * @param name    the imageset name
     * @return the created imageset node
     * @throws RepositoryException when an exception in the repository occurs while creating the node
     */
    public static Node createImagesetNamespace(final Session session, final String prefix, final String name) throws RepositoryException {
        return createImagesetNamespace(session, prefix, name, DEFAULT_IMAGESET_BLUEPRINT);
    }

    /**
     * Create a new imageset namespace node for the imageset with the given prefix and name. The new imageset node will
     * be based on a copy of the template at the given path of a blueprint template.
     *
     * @param session       the JCR session
     * @param prefix        the imageset prefix
     * @param name          the imageset name
     * @param blueprintPath absolute path to the blueprint template node
     * @return the created imageset node
     * @throws RepositoryException when an exception in the repository occurs while creating the node
     */
    public static Node createImagesetNamespace(final Session session, final String prefix, final String name, final String blueprintPath) throws RepositoryException {
        final String destinationImagesetPath = getNamespacePathForImageset(prefix, name);
        if (!session.nodeExists(blueprintPath)) {
            log.warn("Imageset blueprint {} does not exist", blueprintPath);
            return null;
        }
        final Node original = session.getNode(blueprintPath);
        final HippoSession hs = (HippoSession) session;
        final Node imageNode = hs.copy(original, destinationImagesetPath);
        HippoNodeUtils.setSupertype(imageNode, HIPPOGALLERY_IMAGE_SET, HIPPOGALLERY_RELAXED);
        HippoNodeUtils.setUri(imageNode, getGalleryURI(prefix));
        HippoNodeUtils.setNodeType(imageNode, getImagesetName(prefix, name));
        return imageNode;
    }

    /**
     * Returns the name of the node type of the imageset, e.g. prefix:name.
     *
     * @param prefix the imageset prefix
     * @param name   the imageset name
     * @return the name of the imageset node type
     */
    // TODO rename (also used for images)
    public static String getImagesetName(final String prefix, final String name) {
        final StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(':');
        sb.append(name);
        return sb.toString();
    }


    public static boolean imagesetNamespaceNodeExists(final Session session, final String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return false;
        }
        try {
            if (!session.nodeExists(absPath)) {
                return false;
            }
            final Node node = session.getNode(absPath);
            if (node != null) {
                return true;
            }
        } catch (RepositoryException e) {
            log.warn("Error when determining image node");
        }
        return false;
    }

    public static List<Node> getFieldVariantsFromTemplate(final Node imagesetTemplate) throws RepositoryException {
        final List<Node> variants = new ArrayList<>();
        final NodeIterator iterator = imagesetTemplate.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes();
        while (iterator.hasNext()) {
            final Node node = iterator.nextNode();
            if (node.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                final String type = node.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                if(type.contains(":")) {
                    variants.add(node);
                }
            }
        }
        return variants;
    }


    /**
     * Retrieve the default gallery processor node ({@code #GALLERY_PROCESSOR_SERVICE_PATH}) from the repository.
     * It returns null when the node is not available in the repository.
     *
     * @param session the JCR session
     * @return the node of the gallery processor service, or null when not available
     * @throws RepositoryException when exception in repository occurs while retrieving node
     */
    public static Node getGalleryProcessorNode(final Session session) throws RepositoryException {
        if(session.nodeExists(GalleryUtils.GALLERY_PROCESSOR_SERVICE_PATH)) {
            return session.getNode(GalleryUtils.GALLERY_PROCESSOR_SERVICE_PATH);
        }
        log.warn("Default gallery processor not available at {}", GALLERY_PROCESSOR_SERVICE_PATH);
        return null;
    }

    /**
     *
     * @param session
     * @param variant
     * @return
     * @throws RepositoryException
     */
    public static Node getGalleryProcessorVariant(final Session session, final String variant) throws RepositoryException {
        final Node galleryProcessorNode = getGalleryProcessorNode(session);
        if(galleryProcessorNode != null && galleryProcessorNode.hasNode(variant)) {
            return galleryProcessorNode.getNode(variant);
        }
        return null;
    }

    public static String getPathForGalleryProcessorVariant(final String variant) {
        StringBuilder sb = new StringBuilder();
        sb.append(GALLERY_PROCESSOR_SERVICE_PATH);
        sb.append('/');
        sb.append(variant);
        return sb.toString();
    }

    public static String getPathForTemplateVariantField(final Node imagesetTemplate, final String imageName) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append(imagesetTemplate.getPath());
        sb.append('/');
        sb.append("hipposysedit:nodetype");
        sb.append('/');
        sb.append("hipposysedit:nodetype");
        sb.append('/');
        sb.append(imageName);
        return sb.toString();
    }

    public static String getPathForTemplateVariantTemplate(final Node imagesetTemplate, final String imageName) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append(imagesetTemplate.getPath());
        sb.append('/');
        sb.append("editor:templates");
        sb.append('/');
        sb.append("_default_");
        sb.append('/');
        sb.append(imageName);
        return sb.toString();
    }

    private GalleryUtils() {
    }

}
