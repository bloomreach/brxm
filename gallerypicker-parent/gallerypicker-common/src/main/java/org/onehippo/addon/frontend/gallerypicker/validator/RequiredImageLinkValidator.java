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
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.addon.frontend.gallerypicker.GalleryPickerConstants.GALLERY_ROOT_PATH;

public class RequiredImageLinkValidator implements Validator<Node> {

    private static final Logger log = LoggerFactory.getLogger(RequiredImageLinkValidator.class);

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Node value) {
        try {
            if (value.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                final String docBase = value.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                if (StringUtils.isEmpty(docBase)
                        || docBase.equals(JcrConstants.ROOT_NODE_ID)
                        || docBase.equals(galleryRootPathIdentifier(value))) {
                    return Optional.of(context.createViolation());
                }
            }
        } catch (RepositoryException e) {
            log.warn("Could not validate required image link field '{}', assuming it's valid",
                    context.getJcrName(), e);
        }

        return Optional.empty();
    }

    private static String galleryRootPathIdentifier(final Node value) throws RepositoryException {
        final Session session = value.getSession();
        return session.nodeExists(GALLERY_ROOT_PATH)
                ? session.getNode(GALLERY_ROOT_PATH).getIdentifier()
                : null;
    }
}
