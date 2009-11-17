/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.frontend.validation.FieldElement;
import org.hippoecm.frontend.validation.FieldPath;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrFieldValidator implements ITypeValidator {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrFieldValidator.class);

    private IFieldDescriptor field;
    private ITypeDescriptor fieldType;
    private ITypeValidator typeValidator;

    public JcrFieldValidator(IFieldDescriptor field, TypeLocator locator) throws StoreException {
        this.field = field;
        this.fieldType = locator.locate(field.getType());
        if (fieldType.isNode()) {
            typeValidator = new JcrTypeValidator(fieldType, locator);
        }
    }

    public Set<Violation> validate(IModel model) throws ValidationException {
        if (!(model instanceof JcrNodeModel)) {
            throw new ValidationException("Invalid model type; only JcrNodeModel is supported");
        }
        Set<Violation> violations = new HashSet<Violation>();
        Set<String> validators = field.getValidators();
        boolean required = validators.contains("required");
        boolean nonEmpty = validators.contains("non-empty");
        if ((required || nonEmpty) && !field.isProtected()) {
            if ("*".equals(field.getPath())) {
                log.warn("Wildcard properties are not validated");
                return violations;
            }
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            IDataProvider provider;
            if (fieldType.isNode()) {
                provider = new ChildNodeProvider(field, null, nodeModel.getItemModel());
            } else {
                JcrItemModel itemModel = new JcrItemModel(nodeModel.getItemModel().getPath() + "/" + field.getPath());
                provider = new PropertyValueProvider(field, null, itemModel);
            }
            Iterator<?> iter = provider.iterator(0, provider.size());
            if (required && !iter.hasNext()) {
                violations.add(newViolation(field.getPath(), 0, "Required field is not present"));
            }
            while (iter.hasNext()) {
                IModel childModel = provider.model(iter.next());
                if (fieldType.isNode()) {
                    Set<Violation> typeViolations = typeValidator.validate(childModel);
                    if (typeViolations.size() > 0) {
                        String name = field.getPath();
                        if ("*".equals(name)) {
                            JcrNodeModel childNodeModel = (JcrNodeModel) childModel;
                            try {
                                name = childNodeModel.getNode().getName();
                            } catch (RepositoryException e) {
                                throw new ValidationException("Could not resolve path for invalid value", e);
                            }
                        }
                        int index = 0;
                        if (name.indexOf('[') >= 0) {
                            index = Integer.valueOf(name.substring(name.indexOf('[') + 1, name.lastIndexOf(']'))) - 1;
                            name = name.substring(name.indexOf('['));
                        }
                        for (Violation violation : typeViolations) {
                            Set<FieldPath> childPaths = violation.getDependentPaths();
                            Set<FieldPath> paths = new HashSet<FieldPath>();
                            for (FieldPath childPath : childPaths) {
                                FieldElement[] elements = new FieldElement[childPath.getElements().length + 1];
                                System.arraycopy(childPath.getElements(), 0, elements, 1,
                                        childPath.getElements().length);
                                elements[0] = new FieldElement(field, name, index);
                                paths.add(new FieldPath(elements));
                            }
                            violations.add(new Violation(paths, violation.getMessage()));
                        }
                    }
                } else {
                    if (nonEmpty && "String".equals(fieldType.getType())) {
                        String value = (String) childModel.getObject();
                        if ("".equals(value)) {
                            String name = field.getPath();
                            if ("*".equals(name)) {
                                JcrPropertyValueModel valueModel = (JcrPropertyValueModel) childModel;
                                try {
                                    name = valueModel.getJcrPropertymodel().getProperty().getName();
                                } catch (RepositoryException e) {
                                    throw new ValidationException("Could not resolve path for invalid value", e);
                                }
                            }
                            int index = 0;
                            if (name.indexOf('[') >= 0) {
                                index = Integer.valueOf(name.substring(name.indexOf('[') + 1, name.lastIndexOf(']'))) - 1;
                                name = name.substring(name.indexOf('['));
                            }
                            // TODO: add i18n.
                            violations.add(newViolation(name, index, "String value is empty"));
                        }
                    }
                }
            }
        }
        return violations;
    }

    private Violation newViolation(String name, int index, String message) {
        FieldElement[] elements = new FieldElement[1];
        elements[0] = new FieldElement(field, name, index);
        Set<FieldPath> paths = new HashSet<FieldPath>();
        paths.add(new FieldPath(elements));
        return new Violation(paths, new Model(message));
    }

}
