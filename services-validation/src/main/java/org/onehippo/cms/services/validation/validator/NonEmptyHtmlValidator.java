/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.ViolationFactory;
import org.onehippo.cms.services.validation.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for html values.  Verifies that the value is not empty.
 * Use this validator when a customized "Html" type is used.
 * <p>
 * The builtin "Html" type is checked by the {@link NonEmptyValidator} and does not require
 * special treatment.
 */
public class NonEmptyHtmlValidator implements Validator<String> {

    public static final Logger log = LoggerFactory.getLogger(NonEmptyHtmlValidator.class);

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String value,
                                        final ViolationFactory violationFactory) throws ValidationContextException {
        if ("Html".equals(context.getName())) {
            log.warn("Explicit html validation is not necessary for fields of type 'Html'. " +
                    "This is covered by the 'non-empty' validator.");
        }

        if (HtmlUtils.isEmpty(value)) {
            return Optional.of(violationFactory.createViolation());
        }

        return Optional.empty();
    }
}
