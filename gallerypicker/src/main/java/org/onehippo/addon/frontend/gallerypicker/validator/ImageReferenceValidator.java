/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker.validator;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.onehippo.addon.frontend.gallerypicker.GalleryPickerPlugin.GALLERY_ROOT_PATH;
import static org.onehippo.repository.util.JcrConstants.ROOT_NODE_ID;

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
        return value.hasProperty(HIPPO_DOCBASE)
            ? validateDocBase(value.getProperty(HIPPO_DOCBASE).getString(), context)
            : Optional.empty();
    }

    private static Optional<Violation> validateDocBase(final String docBase, final ValidationContext context) throws RepositoryException {
        if (StringUtils.isEmpty(docBase)
                || docBase.equals(ROOT_NODE_ID)
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
