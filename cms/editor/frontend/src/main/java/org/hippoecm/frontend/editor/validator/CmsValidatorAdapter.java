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
package org.hippoecm.frontend.editor.validator;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.cms7.services.validation.Validator;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.field.FieldContext;

import com.google.common.collect.Sets;

public class CmsValidatorAdapter implements ICmsValidator {

    private final Validator<FieldContext, Object> validator;

    public CmsValidatorAdapter(final Validator<FieldContext, Object> validator) {
        this.validator = validator;
    }

    @Override
    public void preValidation(final IFieldValidator fieldValidator) throws ValidationException {
        final FieldContext context = new CmsValidatorFieldContext(fieldValidator);
        try {
            validator.init(context);
        } catch (final InvalidValidatorException e) {
            throw new ValidationException(e);
        }
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator,
                                   final JcrNodeModel parentModel,
                                   final IModel valueModel) throws ValidationException {

        final FieldContext context = new CmsValidatorFieldContext(fieldValidator);
        try {
            final Optional<org.onehippo.cms7.services.validation.Violation> optionalViolation =
                    validator.validate(context, valueModel.getObject());
            if (optionalViolation.isPresent()) {
                final Model<String> message = Model.of(optionalViolation.get().getMessage());
                final Violation violation = fieldValidator.newValueViolation(valueModel, message, FeedbackScope.FIELD);
                return Sets.newHashSet(violation);
            }
        } catch (final ValidatorException e) {
            throw new ValidationException("Error executing validator " + validator, e);
        }

        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return validator.getName();
    }

}
