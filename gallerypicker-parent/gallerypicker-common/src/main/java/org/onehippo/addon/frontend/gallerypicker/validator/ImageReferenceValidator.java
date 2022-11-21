/*
 * Copyright 2019-2022 Bloomreach
 */
package org.onehippo.addon.frontend.gallerypicker.validator;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.addon.frontend.gallerypicker.GalleryPickerConstants.GALLERY_ROOT_PATH;

/**
 * Validator for checking mandatory links to images.
 * <p>
 * It checks the link's reference (a node identifier) for not being set at all or being set to root node '/' or to node
 * '/content/gallery'.
 */
public class ImageReferenceValidator implements Validator<Object>  {

    private static final Logger log = LoggerFactory.getLogger(ImageReferenceValidator.class);

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Object value)
            throws ValidationContextException {

        try {
            if (value instanceof Node) {
                return validateNode((Node) value, context);
            }

            if (value instanceof String) {
                return validateDocBase((String) value, context);
            }
        } catch (final RepositoryException repositoryException) {
            log.error("Error validating image reference field: {}", context.getJcrName(), repositoryException);
        }
        return Optional.empty();
    }

    private static Optional<Violation> validateNode(final Node value, final ValidationContext context) throws RepositoryException {
        return value.hasProperty(HippoNodeType.HIPPO_DOCBASE)
            ? validateDocBase(value.getProperty(HippoNodeType.HIPPO_DOCBASE).getString(), context)
            : Optional.empty();
    }

    private static Optional<Violation> validateDocBase(final String docBase, final ValidationContext context) throws RepositoryException {
        if (StringUtils.isEmpty(docBase)
                || docBase.equals(JcrConstants.ROOT_NODE_ID)
                || docBase.equals(galleryRootPathIdentifier(context))) {
            return Optional.of(context.createViolation());
        }
        return Optional.empty();
    }

    private static String galleryRootPathIdentifier(final ValidationContext context) throws RepositoryException {
        final Session session = context.getParentNode().getSession();
        return session.nodeExists(GALLERY_ROOT_PATH)
            ? session.getNode(GALLERY_ROOT_PATH).getIdentifier()
            : null;
    }
}
