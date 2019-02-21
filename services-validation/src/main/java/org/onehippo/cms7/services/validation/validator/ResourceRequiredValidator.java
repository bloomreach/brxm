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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.field.FieldContext;

/**
 * ResourceRequiredValidator validates fields that are a (subtype of) {@link HippoNodeType#NT_RESOURCE} that a resource
 * has been uploaded.
 *
 */
public class ResourceRequiredValidator extends AbstractFieldValidator<Node> {

    private static final String INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE = "Invalid validation exception. " +
            "A ResourceRequiredValidator can only be used for field types that are a (subtype of) " +
            HippoNodeType.NT_RESOURCE;

    public ResourceRequiredValidator(final ValidatorConfig config) {
        super(config);
    }

    @Override
    public void init(final FieldContext context) throws InvalidValidatorException {
        if (!isAHippoResource(context)) {
            throw new InvalidValidatorException(INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE);
        }
    }

    private boolean isAHippoResource(final FieldContext context) throws InvalidValidatorException {
        final String jcrTypeName = context.getType();
        try {
            final Session jcrSession = context.getJcrSession();
            final NodeTypeManager typeManager = jcrSession.getWorkspace().getNodeTypeManager();
            final NodeType nodeType = typeManager.getNodeType(jcrTypeName);
            return nodeType.isNodeType(HippoNodeType.NT_RESOURCE);
        } catch (final RepositoryException e) {
            throw new InvalidValidatorException("Failed to determine if node type is 'hippo:resource'", e);
        }
    }

    @Override
    public boolean isValid(final FieldContext context, final Node value) throws ValidatorException {
        try {
            final Property resourceData = value.getProperty("jcr:data");
            return resourceData.getLength() > 0;
        } catch (final Exception e) {
            throw new ValidatorException(e);
        }
    }
}
