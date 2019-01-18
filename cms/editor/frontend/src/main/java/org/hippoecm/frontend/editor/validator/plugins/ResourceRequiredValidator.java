/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator.plugins;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.api.HippoNodeType;

/**
  * ResourceRequiredValidator validates fields that are a (subtype of) {@link HippoNodeType.NT_RESOURCE} that a resource
  * has been uploaded.
  *
  * @author David de Bos
  */
public class ResourceRequiredValidator extends AbstractCmsValidator {

    public static final String INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE = "Invalid validation exception. " +
            "A ResourceRequiredValidator can only be used for field types that are a (subtype of) " +
            HippoNodeType.NT_RESOURCE;

    public ResourceRequiredValidator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(final IFieldValidator type) throws ValidationException {
        if (!isAHippoResource(type)) {
            throw new ValidationException(INVALID_VALIDATION_EXCEPTION_ERROR_MESSAGE);
        }
    }

    private boolean isAHippoResource(final IFieldValidator type) throws ValidationException {
        final String jcrTypeName = type.getFieldType().getType();
        try {
            final Session jcrSession = UserSession.get().getJcrSession();
            final NodeTypeManager typeManager = jcrSession.getWorkspace().getNodeTypeManager();
            final NodeType nodeType = typeManager.getNodeType(jcrTypeName);
            return nodeType.isNodeType(HippoNodeType.NT_RESOURCE);
        } catch (final RepositoryException e) {
            throw new ValidationException(e);
        }
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator, final JcrNodeModel model,
                                   final IModel childModel) throws ValidationException {
        final Set<Violation> violations = new HashSet<>();
        try {
            final Node node = ((JcrNodeModel) childModel).getNode();
            final Property resourceData = node.getProperty("jcr:data");
            if (!(resourceData.getLength() > 0)) {
                violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
            }
        } catch (final Exception e) {
            throw new ValidationException(e);
        }

        return violations;
    }

}
