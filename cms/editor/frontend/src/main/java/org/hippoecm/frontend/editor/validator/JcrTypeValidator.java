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
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;

/**
 * Validator for generic jcr node types.
 */
public class JcrTypeValidator implements ITypeValidator {

    private final Set<JcrFieldValidator> fieldValidators = new HashSet<>();
    private final ITypeDescriptor type;
    private final ValidatorService validatorService;

    public JcrTypeValidator(final ITypeDescriptor type, final ValidatorService validatorService) throws StoreException {
        this.type = type;
        this.validatorService = validatorService;
        for (final IFieldDescriptor field : type.getFields().values()) {
            fieldValidators.add(new JcrFieldValidator(field, this));
        }
    }

    public ITypeDescriptor getType() {
        return type;
    }

    public ValidatorService getValidatorService(){
        return validatorService;
    }

    public Set<Violation> validate(final IModel model) throws ValidationException {
        final Set<Violation> violations = new HashSet<>();
        for (final JcrFieldValidator fieldValidator : fieldValidators) {
            final Set<Violation> fieldViolations = fieldValidator.validate(model);
            violations.addAll(fieldViolations);
        }
        return violations;
    }

}
