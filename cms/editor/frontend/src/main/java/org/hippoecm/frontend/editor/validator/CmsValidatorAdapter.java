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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.cms.services.validation.BaseValidationContextImpl;
import org.onehippo.cms.services.validation.api.BaseValidationContext;
import org.onehippo.cms.services.validation.api.ValidationService;
import org.onehippo.cms.services.validation.api.ValidatorInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import static org.onehippo.cms.services.validation.util.ServiceUtils.getValidationService;

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
    public void preValidation(final IFieldValidator fieldValidator) {
        // nothing to do, any pre-validation is done as part of the validate() call
    }

    private static ValidatorInstance getValidator(final String name) {
        final ValidationService validationService = getValidationService();
        final ValidatorInstance validator = validationService.getValidator(name);

        if (validator == null) {
            log.warn("Failed to retrieve validator '{}' from validation module", name);
        }

        return validator;
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator,
                                   final JcrNodeModel parentModel,
                                   final IModel valueModel) throws ValidationException {

        final ValidatorInstance validator = getValidator(name);
        if (validator == null) {
            return Collections.emptySet();
        }

        final BaseValidationContext context = createValidationContext(fieldValidator, parentModel.getObject());
        try {
            final Optional<org.onehippo.cms.services.validation.api.Violation> violation = validator.validate(context, valueModel.getObject());
            return violation.isPresent()
                    ? getViolations(fieldValidator, valueModel, violation.get())
                    : Collections.emptySet();

        } catch (final RuntimeException e) {
            throw new ValidationException("Error executing validator " + name, e);
        }
    }

    private static BaseValidationContext createValidationContext(final IFieldValidator fieldValidator, final Node parentNode) {
        final ITypeDescriptor fieldType = fieldValidator.getFieldType();
        final String name = fieldType.getName();
        final String type = fieldType.getType();
        final UserSession userSession = UserSession.get();
        final Locale locale = userSession.getLocale();
        final TimeZone timeZone = userSession.getTimeZone();
        return new BaseValidationContextImpl(name, type, locale, timeZone, parentNode);
    }

    private static Set<Violation> getViolations(final IFieldValidator fieldValidator, final IModel valueModel,
                                                final org.onehippo.cms.services.validation.api.Violation violation) throws ValidationException {
        final Model<String> message = Model.of(violation.getMessage());
        return Sets.newHashSet(fieldValidator.newValueViolation(valueModel, message, FeedbackScope.FIELD));
    }

}
