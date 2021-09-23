/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.cms.services.validation.api.internal.ValueContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtils.class);

    private ValidationUtils() {
    }

    /**
     * Validates all fields of a document and its type.
     * Field violations will be reported in the {@link FieldValue} objects of the document.
     * Type violations will be reported in the {@link DocumentInfo} object of the document.
     *
     * @param document the data of all fields
     * @param docType the document type
     * @param draftNode the draft node of the document
     * @param userContext the user context
     *
     * @return true if no violations have been found, false otherwise
     */
    public static boolean validateDocument(final Document document,
                                           final DocumentType docType,
                                           final Node draftNode,
                                           final UserContext userContext) {
        final CompoundContext documentContext = new CompoundContext(draftNode, draftNode, userContext.getLocale(), userContext.getTimeZone());
        final int fieldViolationCount = FieldTypeUtils.validateFieldValues(document.getFields(), docType.getFields(), documentContext);

        final int typeViolationCount = validateType(document.getInfo(), docType, draftNode, userContext);

        final int totalViolationCount = fieldViolationCount + typeViolationCount;
        document.getInfo().setErrorCount(totalViolationCount);

        return totalViolationCount == 0;
    }

    private static int validateType(final DocumentInfo documentInfo,
                                    final DocumentType docType,
                                    final Node draftNode,
                                    final UserContext userContext) {
        final Set<String> validatorNames = docType.getValidatorNames();
        if (validatorNames.isEmpty()) {
            return 0;
        }

        final ValueContext typeContext = createTypeValidationContext(docType, draftNode, userContext);
        final List<String> errorMessages = new ArrayList<>();

        for (String validatorName : validatorNames) {
            validateType(validatorName, typeContext, draftNode)
                    .ifPresent(errorMessages::add);
        }

        if (!errorMessages.isEmpty()) {
            documentInfo.setErrorMessages(errorMessages);
        }

        return errorMessages.size();
    }

    private static ValueContext createTypeValidationContext(final DocumentType docType,
                                                            final Node draftNode,
                                                            final UserContext userContext) {
        final String jcrType = docType.getId();
        final Locale locale = userContext.getLocale();
        final TimeZone timeZone = userContext.getTimeZone();

        try {
            final String jcrName = draftNode.getName();
            final Node parent = draftNode.getParent();
            return new ValueContextImpl(jcrName, jcrType, jcrType, draftNode, parent, locale, timeZone);
        } catch (RepositoryException e) {
            log.error("Cannot create validation context for document type '{}'", jcrType, e);
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    private static Optional<String> validateType(final String validatorName, final ValueContext context, final Node node) {
        final ValidatorInstance validator = FieldTypeUtils.getValidator(validatorName);
        if (validator == null) {
            log.warn("Failed to find validator '{}', ignoring it", validatorName);
            return Optional.empty();
        }

        try {
            final Optional<Violation> violation = validator.validate(context, node);
            return violation.map(Violation::getMessage);
        } catch (RuntimeException e) {
            log.warn("Error while validating node '{}' with validator '{}', assuming it's valid",
                    JcrUtils.getNodePathQuietly(node), validatorName, e);
            return Optional.empty();
        }

    }

    /**
     * Validates the value of a field with all configured validators.
     *
     * @param value             the field value wrapper
     * @param context           the field context
     * @param validatorNames    the names of the validators to use
     * @param validatedValue    the actual validated value
     *
     * @return 1 if a validator deemed the value invalid, 0 otherwise
     */
    public static int validateValue(final FieldValue value,
                                    final ValueContext context,
                                    final Set<String> validatorNames,
                                    final Object validatedValue) {
        return validatorNames.stream()
                .allMatch(validatorName -> validateValue(value, context, validatorName, validatedValue))
                ? 0 : 1;
    }

    /**
     * Validates the value of a field with a validator.
     *
     * @param value             the field value wrapper
     * @param context           the field context
     * @param validatorName     the name of the validator to use
     * @param validatedValue    the actual validated value
     *
     * @return whether the validator deemed the value valid
     */
    private static boolean validateValue(final FieldValue value,
                                        final ValueContext context,
                                        final String validatorName,
                                        final Object validatedValue) {
        final ValidatorInstance validator = FieldTypeUtils.getValidator(validatorName);
        if (validator == null) {
            log.warn("Failed to find validator '{}', ignoring it", validatorName);
            return true;
        }

        try {
            final Optional<Violation> violation = validator.validate(context, validatedValue);

            violation.ifPresent((error) -> {
                final ValidationErrorInfo errorInfo = new ValidationErrorInfo(validatorName, error.getMessage());
                value.setErrorInfo(errorInfo);
            });

            return !violation.isPresent();
        } catch (RuntimeException e) {
            log.warn("Error while validating field '{}' of type '{}', assuming it's valid",
                    context.getJcrName(), context.getJcrType(), e);
            return true;
        }
    }
}
