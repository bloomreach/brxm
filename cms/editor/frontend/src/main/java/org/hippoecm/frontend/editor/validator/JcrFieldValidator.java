/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrFieldValidator implements ITypeValidator, IFieldValidator {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrFieldValidator.class);

    public static final String REQUIRED_FIELD_NOT_PRESENT = "required-field-not-present";

    private IFieldDescriptor field;
    private ITypeDescriptor fieldType;
    private ITypeValidator typeValidator;
    private ValidatorService validatorService;

    public JcrFieldValidator(IFieldDescriptor field, JcrTypeValidator container) throws StoreException {
        this.field = field;
        this.validatorService = container.getValidatorService();
        this.fieldType = field.getTypeDescriptor();
        if (fieldType.isNode()) {
            if (fieldType.equals(container.getType())) {
                typeValidator = container;
            } else {
                typeValidator = new JcrTypeValidator(fieldType, validatorService);
            }
        }

        if (validatorService != null) {
            Set<String> validators = field.getValidators();
            if (!validators.isEmpty()) {
                for (String fieldValidatorType : validators) {
                    if (validatorService.containsValidator(fieldValidatorType)) {
                        try {
                            validatorService.getValidator(fieldValidatorType).preValidation(this);
                        } catch (ValidationException e) {
                            log.error("Configuration is inconsistent", e);
                        }
                    }
                }
            }
        }
    }

    public Set<Violation> validate(IModel model) throws ValidationException {
        if (!(model instanceof JcrNodeModel)) {
            throw new ValidationException("Invalid model type; only JcrNodeModel is supported");
        }
        Set<Violation> violations = new HashSet<Violation>();
        Set<String> validators = field.getValidators();
        boolean required = validators.contains("required");
        if ((required || fieldType.isNode() || validators.size() > 0) && !field.isProtected()) {
            if ("*".equals(field.getPath())) {
                if (log.isDebugEnabled() && validators.size() > 0) {
                    log.debug("Wildcard properties are not validated");
                }
                return violations;
            }
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            AbstractProvider<?, ? extends IModel> provider;
            if (fieldType.isNode()) {
                provider = new ChildNodeProvider(field, null, nodeModel.getItemModel());
            } else {
                JcrItemModel itemModel = new JcrItemModel(nodeModel.getItemModel().getPath() + "/" + field.getPath(), true);
                provider = new PropertyValueProvider(field, null, itemModel);
            }
            Iterator<? extends IModel> iter = provider.iterator(0, provider.size());
            if (required && !iter.hasNext()) {
                violations.add(newViolation(field.getPath(), 0, REQUIRED_FIELD_NOT_PRESENT, null));
            }
            while (iter.hasNext()) {
                IModel childModel = iter.next();
                if (fieldType.isNode()) {
                    if (required || field.getTypeDescriptor().isValidationCascaded()) {
                        Set<Violation> typeViolations = typeValidator.validate(childModel);
                        if (typeViolations.size() > 0) {
                            addTypeViolations(violations, childModel, typeViolations);
                        }
                    }
                }
                if (validatorService != null) {
                    for (String fieldValidatorType : validators) {
                        if (validatorService.containsValidator(fieldValidatorType)) {
                            violations.addAll(validatorService.getValidator(fieldValidatorType).validate(this, nodeModel, childModel));
                        }
                    }
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

    private void addTypeViolations(Set<Violation> violations, IModel childModel, Set<Violation> typeViolations)
            throws ValidationException {
    	JcrNodeModel childNodeModel = (JcrNodeModel) childModel;
        String name = field.getPath();
        if ("*".equals(name)) {
            try {
                name = childNodeModel.getNode().getName();
            } catch (RepositoryException e) {
                throw new ValidationException("Could not resolve path for invalid value", e);
            }
        }
        int index = 0;
        try {
        	index = childNodeModel.getNode().getIndex() - 1;
        } catch (RepositoryException e) {
            throw new ValidationException("Could not resolve path for invalid value", e);
        }

        for (Violation violation : typeViolations) {
            Set<ModelPath> childPaths = violation.getDependentPaths();
            Set<ModelPath> paths = new HashSet<ModelPath>();
            for (ModelPath childPath : childPaths) {
                ModelPathElement[] elements = new ModelPathElement[childPath.getElements().length + 1];
                System.arraycopy(childPath.getElements(), 0, elements, 1, childPath.getElements().length);
                elements[0] = new ModelPathElement(field, name, index);
                paths.add(new ModelPath(elements));
            }
            violations.add(new Violation(paths, violation.getMessageKey(), violation.getParameters()));
        }
    }

    public Violation newValueViolation(IModel childModel, String key) throws ValidationException {
        String name = field.getPath();
        int index = 0;
        if (childModel instanceof JcrPropertyValueModel) {
            JcrPropertyValueModel valueModel = (JcrPropertyValueModel) childModel;
            if ("*".equals(name)) {
                try {
                    name = valueModel.getJcrPropertymodel().getProperty().getName();
                } catch (RepositoryException e) {
                    throw new ValidationException("Could not resolve path for invalid value", e);
                }
            }
            index = valueModel.getIndex();
            if (index == -1) {
                index = 0;
            }
        }
        return newViolation(name, index, key, null);
    }

    public Violation newViolation(String name, int index, String message, Object[] parameters) {
        ModelPathElement[] elements = new ModelPathElement[1];
        elements[0] = new ModelPathElement(field, name, index);
        Set<ModelPath> paths = new HashSet<ModelPath>();
        paths.add(new ModelPath(elements));
        return new Violation(paths, message, parameters);
    }

}
