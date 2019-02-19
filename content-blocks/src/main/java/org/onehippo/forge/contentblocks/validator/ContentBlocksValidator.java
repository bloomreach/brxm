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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.editor.type.JcrTypeLocator;
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
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for the Content Blocks. The validator delegates to the correct validators based on the content block
 * document type.
 */
public class ContentBlocksValidator extends AbstractCmsValidator {

    private static Logger log = LoggerFactory.getLogger(ContentBlocksValidator.class);

    public ContentBlocksValidator(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }


    @Override
    public void preValidation(IFieldValidator type) throws ValidationException {
        //Do nothing as we configure the validator ourselves.
    }

    @Override
    public Set<Violation> validate(final IFieldValidator iFieldValidator, final JcrNodeModel jcrNodeModel,
                                   final IModel iModel) throws ValidationException {

        try {
            // get descriptor based on actual node type
            final Node contentBlockNode = (Node) iModel.getObject();
            final String primaryNodeType = contentBlockNode.getPrimaryNodeType().getName();

            final JcrTypeLocator jcrTypeLocator = new JcrTypeLocator();
            final ITypeDescriptor contentBlockDescriptor = jcrTypeLocator.locate(primaryNodeType);
            log.debug("Content block descriptor {} found by type {}", contentBlockDescriptor, primaryNodeType);

            if (contentBlockDescriptor != null) {
                ValidatorService validatorService = getPluginContext().getService("field.validator.service",
                        ValidatorService.class);
                if (validatorService == null) {
                    log.error("ValidatorService is not found by 'field.validator.service': " +
                            "cannot validate content block node {}", contentBlockNode.getPath());
                } else {
                    // delegate to the actual content block's validator
                    final JcrTypeValidator validator = new JcrTypeValidator(contentBlockDescriptor, validatorService);
                    final Set<Violation> violations = validator.validate(new JcrNodeModel(contentBlockNode));

                    final IFieldDescriptor fieldDescriptor = iFieldValidator.getFieldDescriptor();
                    return prependFieldPathToViolations(violations, contentBlockNode, fieldDescriptor);
                }
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException occurred accessing node", e);
        } catch (StoreException e) {
            log.warn("StoreException occurred during locating the correct jcr type", e);
        }

        // no validation done
        return new HashSet<>();
    }

    /**
     * Correct the paths of the violations, that are based on the type only, by prepending with the content blocks
     * compound path.
     */
    protected Set<Violation> prependFieldPathToViolations(final Set<Violation> violations, final Node contentBlockNode,
                                                          final IFieldDescriptor fieldDescriptor)
            throws ValidationException {

        // correct node index on behalf on multiples (which content blocks are)
        int index;
        try {
            index = contentBlockNode.getIndex() - 1;
        } catch (RepositoryException e) {
            throw new ValidationException("Could not get index for node", e);
        }

        // replace the violations by violations that have also the path element of the content block compound in them
        final Set<Violation> newViolations = new HashSet<>(violations.size());
        for (Violation violation : violations) {
            Set<ModelPath> childPaths = violation.getDependentPaths();
            Set<ModelPath> newPaths = new HashSet<>();
            for (ModelPath childPath : childPaths) {
                ModelPathElement[] elements = new ModelPathElement[childPath.getElements().length + 1];
                System.arraycopy(childPath.getElements(), 0, elements, 1, childPath.getElements().length);
                elements[0] = new ModelPathElement(fieldDescriptor, fieldDescriptor.getPath(), index);
                newPaths.add(new ModelPath(elements));
            }
            newViolations.add(new Violation(newPaths, violation.getMessage(), violation.getFeedbackScope()));
        }
        return newViolations;
    }
}
