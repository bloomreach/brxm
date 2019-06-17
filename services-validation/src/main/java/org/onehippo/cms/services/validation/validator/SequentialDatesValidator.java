/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.services.validation.validator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.DateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document or compound-level validator to check if dates are in sequence.
 * <p>
 * During validation, all dates are read off the to-be-validated node, deeper nesting is not supported by this
 * validator. The date property names (and order) must be specified with a multivalued String property
 * 'datePropertyNames' property on the validator's configuration node (project-specific). Missing or empty date
 * properties are ignored, those should be addressed through the 'required' validator.
 */
public class SequentialDatesValidator implements Validator<Node> {

    private static final Logger log = LoggerFactory.getLogger(SequentialDatesValidator.class);

    private static final String DATES_PROPERTY = "datePropertyNames";

    private List<String> datePropertyNames = new ArrayList<>();

    public SequentialDatesValidator(final Node config) {
        try {
            if (!config.hasProperty(DATES_PROPERTY)) {
                log.error("Incorrect configuration of SequentialDatesValidator at {}. No property '{}' configured.", 
                        config.getPath(), DATES_PROPERTY);
                return;
            }

            final Value[] datePropertyValues = config.getProperty(DATES_PROPERTY).getValues();
            if (datePropertyValues.length < 2) {
                log.error("Incorrect configuration of SequentialDatesValidator at {}. Property '{}' must have at " +
                        "least 2 values.", config.getPath(), DATES_PROPERTY);
                return;
            }

            for (Value datePropertyValue : datePropertyValues) {
                final String datePropertyName = datePropertyValue.getString();
                if (!datePropertyName.isEmpty()) {
                    datePropertyNames.add(datePropertyName);
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to read validator configuration.", e);
        }
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Node node) {
        Calendar earlierDate = null;

        try {
            for (String datePropertyName : datePropertyNames) {
                if (node.hasProperty(datePropertyName)) {
                    final Calendar laterDate = node.getProperty(datePropertyName).getDate();
                    if (!laterDate.getTime().equals(DateConstants.EMPTY_DATE)) {
                        if (earlierDate != null && !laterDate.after(earlierDate)) {
                            return Optional.of(context.createViolation());
                        }
                        earlierDate = laterDate;
                    }
                }
            }
        } catch (ValueFormatException e) {
            log.warn("Cannot parse date value", e);
        } catch (RepositoryException e) {
            log.error("Error retrieving date values", e);
        }

        return Optional.empty();
    }
}
