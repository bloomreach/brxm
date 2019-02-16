/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.cms7.services.validation.ValidationScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.validation.ValidatorUtils.REQUIRED_VALIDATOR;

public class JcrFieldValidator implements ITypeValidator, IFieldValidator {

    private static final Logger log = LoggerFactory.getLogger(JcrFieldValidator.class);

    private final IFieldDescriptor field;
    private final ITypeDescriptor fieldType;
    private final ValidatorService validatorService;
    private ITypeValidator typeValidator;

    JcrFieldValidator(final IFieldDescriptor field, final JcrTypeValidator container) throws StoreException {
        this.field = field;
        this.validatorService = container.getValidatorService();
        this.fieldType = field.getTypeDescriptor();

        if (fieldType.isNode()) {
            typeValidator = fieldType.equals(container.getType())
                    ? container
                    : new JcrTypeValidator(fieldType, validatorService);
        }

        if (validatorService != null) {
            final Set<String> validators = field.getValidators();
            for (final String fieldValidatorType : validators) {
                if (validatorService.containsValidator(fieldValidatorType)) {
                    try {
                        validatorService.getValidator(fieldValidatorType).preValidation(this);
                    } catch (final ValidationException e) {
                        log.error("Configuration is inconsistent", e);
                    }
                }
            }
        }
    }

    public Set<Violation> validate(final IModel model) throws ValidationException {
        if (!(model instanceof JcrNodeModel)) {
            throw new ValidationException("Invalid model type; only JcrNodeModel is supported");
        }
        final Set<Violation> violations = new LinkedHashSet<>();
        final Set<String> validators = field.getValidators();
        final boolean required = validators.contains(REQUIRED_VALIDATOR);
        if ((required || fieldType.isNode() || !validators.isEmpty()) && !field.isProtected()) {
            if ("*".equals(field.getPath())) {
                if (log.isDebugEnabled() && !validators.isEmpty()) {
                    log.debug("Wildcard properties are not validated");
                }
                return violations;
            }
            final JcrNodeModel nodeModel = (JcrNodeModel) model;
            final AbstractProvider<?, ? extends IModel> provider;
            if (fieldType.isNode()) {
                provider = new ChildNodeProvider(field, null, nodeModel.getItemModel());
            } else {
                final String propertyPath = nodeModel.getItemModel().getPath() + "/" + field.getPath();
                final JcrItemModel<Property> itemModel = new JcrItemModel<>(propertyPath, true);
                provider = new PropertyValueProvider(field, null, itemModel);
            }
            final Iterator<? extends IModel> iter = provider.iterator(0, provider.size());
            if (required && !iter.hasNext()) {
                violations.add(newViolation(
                        new ModelPathElement(field, field.getPath(), 0),
                        getMessage(ValidatorMessages.REQUIRED_FIELD_NOT_PRESENT),
                        ValidationScope.FIELD)
                );
            }

            while (iter.hasNext()) {
                final IModel childModel = iter.next();
                if (fieldType.isNode()) {
                    if (required || field.getTypeDescriptor().isValidationCascaded()) {
                        final Set<Violation> typeViolations = typeValidator.validate(childModel);
                        if (typeViolations.size() > 0) {
                            addTypeViolations(violations, childModel, typeViolations);
                        }
                    }
                }

                if (validatorService != null) {
                    for (final String fieldValidatorType : validators) {
                        if (validatorService.containsValidator(fieldValidatorType)) {
                            violations.addAll(validatorService.getValidator(fieldValidatorType)
                                              .validate(this, nodeModel, childModel));
                        }
                    }
                }

                if (required && field.getTypeDescriptor().isType("Date")
                    && PropertyValueProvider.EMPTY_DATE.equals(childModel.getObject())) {
                        violations.add(newViolation(
                                new ModelPathElement(field, field.getPath(), 0),
                                getMessage(ValidatorMessages.REQUIRED_FIELD_NOT_PRESENT),
                                ValidationScope.FIELD)
                        );
                }
            }
        }
        return violations;
    }

    public IFieldDescriptor getFieldDescriptor() {
        return field;
    }

    public ITypeDescriptor getFieldType() {
        return fieldType;
    }

    @Override
    public Violation newValueViolation(final IModel childModel, final String key) throws ValidationException {
        return newValueViolation(childModel, getMessage(key));
    }

    @Override
    public Violation newValueViolation(final IModel childModel, final IModel<String> message)
            throws ValidationException {
        return newValueViolation(childModel, message, ValidationScope.FIELD);
    }

    @Override
    public Violation newValueViolation(final IModel childModel, final IModel<String> message,
                                       final ValidationScope scope) throws ValidationException {
        return newViolation(getElement(childModel), message, scope);
    }

    private void addTypeViolations(final Set<Violation> violations, final IModel childModel, final Set<Violation> typeViolations)
            throws ValidationException {
        final JcrNodeModel childNodeModel = (JcrNodeModel) childModel;
        String name = field.getPath();
        if ("*".equals(name)) {
            try {
                name = childNodeModel.getNode().getName();
            } catch (final RepositoryException e) {
                throw new ValidationException("Could not resolve path for invalid value", e);
            }
        }
        final int index;
        try {
            index = childNodeModel.getNode().getIndex() - 1;
        } catch (final RepositoryException e) {
            throw new ValidationException("Could not resolve path for invalid value", e);
        }

        for (final Violation typeViolation : typeViolations) {
            final Set<ModelPath> childPaths = typeViolation.getDependentPaths();
            final Set<ModelPath> paths = new HashSet<>();
            for (final ModelPath childPath : childPaths) {
                final ModelPathElement[] elements = new ModelPathElement[childPath.getElements().length + 1];
                System.arraycopy(childPath.getElements(), 0, elements, 1, childPath.getElements().length);
                elements[0] = new ModelPathElement(field, name, index);
                paths.add(new ModelPath(elements));
            }
            violations.add(new Violation(paths, typeViolation.getMessage(), typeViolation.getValidationScope()));
        }
    }

    private ModelPathElement getElement(final IModel childModel) throws ValidationException {
        String name = field.getPath();
        int index = 0;
        if (childModel instanceof JcrPropertyValueModel) {
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) childModel;
            if ("*".equals(name)) {
                try {
                    name = valueModel.getJcrPropertymodel().getProperty().getName();
                } catch (final RepositoryException e) {
                    throw new ValidationException("Could not resolve path for invalid value", e);
                }
            }
            index = valueModel.getIndex();
            if (index == -1) {
                index = 0;
            }
        }
        return new ModelPathElement(field, name, index);
    }

    private IModel<String> getMessage(final String key, final Object... parameters) {
        return new ClassResourceModel(key, ValidatorMessages.class, parameters);
    }

    public Violation newViolation(final ModelPathElement child, final String message, final Object[] parameters,
                                  final ValidationScope scope) {
        final Set<ModelPath> paths = getModelPaths(child);
        return new Violation(paths, getMessage(message, parameters), scope);
    }

    public Violation newViolation(final ModelPathElement child, final IModel<String> messageModel,
                                  final ValidationScope scope) {
        final Set<ModelPath> paths = getModelPaths(child);
        return new Violation(paths, messageModel, scope);
    }

    private Set<ModelPath> getModelPaths(final ModelPathElement child) {
        final ModelPathElement[] elements = new ModelPathElement[1];
        elements[0] = child;
        final Set<ModelPath> paths = new HashSet<>();
        paths.add(new ModelPath(elements));
        return paths;
    }

}
