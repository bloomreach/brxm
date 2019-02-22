/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.contentblocks.validator;

import java.util.Collections;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.editor.type.JcrTypeLocator;
import org.hippoecm.frontend.editor.validator.JcrFieldValidator;
import org.hippoecm.frontend.editor.validator.JcrTypeValidator;
import org.hippoecm.frontend.editor.validator.ValidatorService;
import org.hippoecm.frontend.editor.validator.plugins.AbstractCmsValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for the Content Blocks. The validator delegates to the correct validators based on the content block
 * document type.
 */
public class ContentBlocksValidator extends AbstractCmsValidator {

    private static final Logger log = LoggerFactory.getLogger(ContentBlocksValidator.class);

    public ContentBlocksValidator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }


    @Override
    public void preValidation(final IFieldValidator type) throws ValidationException {
        //Do nothing as we configure the validator ourselves.
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator, final JcrNodeModel jcrNodeModel,
                                   final IModel model) throws ValidationException {

        final Node contentBlockNode = (Node) model.getObject();
        final ITypeDescriptor contentBlockDescriptor = getContentBlockDescriptor(contentBlockNode);
        if (contentBlockDescriptor == null) {
            return Collections.emptySet();
        }

        final ValidatorService validatorService = getPluginContext().getService(
                ValidatorService.DEFAULT_FIELD_VALIDATOR_SERVICE, ValidatorService.class);
        if (validatorService == null) {
            log.error("ValidatorService is not found by '{}': cannot validate content block node {}",
                    ValidatorService.DEFAULT_FIELD_VALIDATOR_SERVICE, JcrUtils.getNodePathQuietly(contentBlockNode));
            return Collections.emptySet();
        }

        // delegate to the actual content block's validator
        final JcrTypeValidator validator = getTypeValidator(contentBlockDescriptor, validatorService);
        if (validator == null) {
            return Collections.emptySet();
        }

        final Set<Violation> violations = validator.validate(new JcrNodeModel(contentBlockNode));

        // correct node index on behalf of multiples (which content blocks are)
        final int index;
        try {
            index = contentBlockNode.getIndex() - 1;
        } catch (final RepositoryException e) {
            throw new ValidationException("Could not get index for node", e);
        }

        // Correct the paths of the violations, that are based on the type only, by prepending with the content
        // blocks compound path.
        final IFieldDescriptor fieldDescriptor = fieldValidator.getFieldDescriptor();
        return JcrFieldValidator.prependFieldPathToViolations(violations, fieldDescriptor, fieldDescriptor.getPath(),
                index);
    }

    // get descriptor based on actual node type
    private static ITypeDescriptor getContentBlockDescriptor(final Node contentBlockNode) {
        final String primaryNodeType;
        try {
            primaryNodeType = contentBlockNode.getPrimaryNodeType().getName();
            final JcrTypeLocator jcrTypeLocator = new JcrTypeLocator();
            final ITypeDescriptor contentBlockDescriptor = jcrTypeLocator.locate(primaryNodeType);
            log.debug("Content block descriptor {} found by type '{}'", contentBlockDescriptor, primaryNodeType);
            return contentBlockDescriptor;
        } catch (final RepositoryException e) {
            log.warn("Failed to get the primary node type from content block node", e);
        } catch (final StoreException e) {
            log.warn("An error occurred while locating the correct JCR type", e);
        }
        return null;
    }

    private static JcrTypeValidator getTypeValidator(final ITypeDescriptor contentBlockDescriptor,
                                                     final ValidatorService validatorService) {
        try {
            return new JcrTypeValidator(contentBlockDescriptor, validatorService);
        } catch (final StoreException e) {
            log.warn("StoreException occurred during locating the correct jcr type", e);
        }
        return null;
    }

}
