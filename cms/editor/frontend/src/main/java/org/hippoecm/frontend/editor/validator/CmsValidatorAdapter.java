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
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.cms7.services.validation.ValidationService;
import org.onehippo.cms7.services.validation.Validator;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import static org.onehippo.cms7.services.validation.util.ServiceUtils.getValidationService;

public class CmsValidatorAdapter implements ICmsValidator {

    public static final Logger log = LoggerFactory.getLogger(CmsValidatorAdapter.class);

    private final String name;

    public CmsValidatorAdapter(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void preValidation(final IFieldValidator fieldValidator) throws ValidationException {
        final Validator validator = getValidator(name);
        if (validator == null) {
            return;
        }

        final ValidatorContext context = new CmsValidatorFieldContext(fieldValidator);
        try {
            validator.init(context);
        } catch (final InvalidValidatorException e) {
            throw new ValidationException(e);
        }
    }

    private static Validator getValidator(final String name) {
        final ValidationService validationService = getValidationService();
        final Validator validator = validationService.getValidator(name);
        if (validator == null) {
            log.warn("Failed to retrieve validator[{}] from validation module", name);
        }
        return validator;
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator,
                                   final JcrNodeModel parentModel,
                                   final IModel valueModel) throws ValidationException {

        final Validator validator = getValidator(name);
        if (validator == null) {
            return Collections.emptySet();
        }

        final String value = getString(valueModel);
        final ValidatorContext context = new CmsValidatorFieldContext(fieldValidator);
        try {
            final Optional<org.onehippo.cms7.services.validation.Violation> violation = validator.validate(context, value);
            return violation.isPresent()
                    ? getViolations(fieldValidator, valueModel, violation.get())
                    : Collections.emptySet();

        } catch (final RuntimeException e) {
            throw new ValidationException("Error executing validator " + name, e);
        }
    }

    private static String getString(final IModel valueModel) {
        final Object object = valueModel.getObject();
        return object != null ? object.toString() : null;
    }

    private static Set<Violation> getViolations(final IFieldValidator fieldValidator, final IModel valueModel,
                                                final org.onehippo.cms7.services.validation.Violation violation) throws ValidationException {
        final Model<String> message = Model.of(violation.getMessage());
        return Sets.newHashSet(fieldValidator.newValueViolation(valueModel, message, FeedbackScope.FIELD));
    }

}
