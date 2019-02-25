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
package org.onehippo.cms7.services.validation.validator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.addon.frontend.gallerypicker.GalleryPickerPlugin;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.field.FieldContext;
import org.onehippo.repository.util.JcrConstants;

/**
 * Validator for checking mandatory links to images.
 * <p>
 * It checks the link's reference (a node identifier) for not being set at all or being set to root node '/' or to node
 * '/content/gallery'.
 */
public class ImageReferenceValidator extends AbstractFieldValidator<Object> {

    private static final String INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE = "Invalid validation exception. " +
            "An ImageReferenceValidator can only be used for field types that are a (subtype of) " +
            HippoNodeType.NT_FACETSELECT + " or " + HippoNodeType.NT_MIRROR;

    public ImageReferenceValidator(final ValidatorConfig config) {
        super(config);
    }

    @Override
    public void init(final FieldContext context) throws InvalidValidatorException {
        if (!isOfCorrectType(context)) {
            throw new InvalidValidatorException(INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE);
        }
    }

    private boolean isOfCorrectType(final FieldContext context) throws InvalidValidatorException {
        final String jcrTypeName = context.getType();
        try {
            final Session jcrSession = context.getJcrSession();
            final NodeTypeManager typeManager = jcrSession.getWorkspace().getNodeTypeManager();
            final NodeType nodeType = typeManager.getNodeType(jcrTypeName);
            return nodeType.isNodeType(HippoNodeType.NT_FACETSELECT) || nodeType.isNodeType(HippoNodeType.NT_MIRROR);
        } catch (final RepositoryException e) {
            throw new InvalidValidatorException("Failed to determine if node type is of the correct type", e);
        }
    }

    @Override
    protected boolean isValid(final FieldContext context, final Object object) throws ValidatorException {
        try {
            final String referencedIdentifier = getReferencedIdentifier(object);
            if (StringUtils.isBlank(referencedIdentifier) || referencedIdentifier.equals(JcrConstants.ROOT_NODE_ID)) {
                return false;
            }

            final String galleryRootNodeIdentifier = getGalleryRootNodeIdentifier(context.getJcrSession());
            return !referencedIdentifier.equals(galleryRootNodeIdentifier);
        } catch (RepositoryException e) {
            throw new ValidatorException("Error validating field '" + context.getName() + "' with " + object, e);
        }
    }

    private String getReferencedIdentifier(final Object object) throws RepositoryException, ValidatorException {
        if (object instanceof Node) {
            Node node = (Node) object;
            if (node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                return node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            }
            throw new ValidatorException("Node does not have property '" + HippoNodeType.HIPPO_DOCBASE + "' at path " +
                                         JcrUtils.getNodePathQuietly(node));
        } else if (object instanceof String) {
            return (String) object;
        }
        throw new ValidatorException("Can only validate Node or String objects.");
    }

    protected String getGalleryRootNodeIdentifier(final Session jcrSession) throws RepositoryException {
        if (jcrSession.nodeExists(GalleryPickerPlugin.GALLERY_ROOT_PATH)) {
            return jcrSession.getNode(GalleryPickerPlugin.GALLERY_ROOT_PATH).getIdentifier();
        }
        return null;
    }

}
