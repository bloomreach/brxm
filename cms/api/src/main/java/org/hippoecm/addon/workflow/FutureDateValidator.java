/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.time.Instant;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;

/**
 * Validate if the date value is in the future. The fraction time after minute is not counted.
 *
 */
public class FutureDateValidator implements IValidator<Date> {

    public static final String EMPTY_DATE = "date.empty";
    public static final String DATE_IN_THE_PAST = "date.in.past";
    public static final String INPUTDATE_LABEL = "inputdate";

    @Override
    public void validate(final IValidatable<Date> validatable) {
        final Date date = validatable.getValue();
        if (date == null) {
            final ValidationError emptyDate = new ValidationError(this).addKey(EMPTY_DATE);
            validatable.error(emptyDate);
            return;
        }

        final Instant publicationDateTime = date.toInstant()
                .plus(1, ChronoUnit.MINUTES); // 1 minute up to round up second fraction.

        final Instant now = Instant.now();

        if (publicationDateTime.isBefore(now)) {
            final ValidationError dateInPast = new ValidationError(this).addKey(DATE_IN_THE_PAST);
            final String dateLabel = DateTimePrinter.of(date).print(FormatStyle.LONG, FormatStyle.MEDIUM);
            dateInPast.setVariable(INPUTDATE_LABEL, dateLabel);
            validatable.error(dateInPast);
        }
    }
}
