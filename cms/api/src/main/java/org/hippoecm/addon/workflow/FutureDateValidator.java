/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;

/**
 * Validate if the date value is in the future. The fraction time after minute is not counted.
 *
 */
public class FutureDateValidator extends AbstractValidator<Date> {

    public static final String EMPTY_DATE = "date.empty";
    public static final String DATE_IN_THE_PAST = "date.in.past";
    public static final String INPUTDATE_LABEL = "inputdate";

    private String resourceKey;

    public FutureDateValidator() {
    }

    public boolean validateOnNullValue() {
        return true;
    }

    @Override
    protected void onValidate(final IValidatable<Date> dateIValidatable) {
        final Date date = dateIValidatable.getValue();
        if (date == null) {
            resourceKey = EMPTY_DATE;
            error(dateIValidatable);
            return;
        }

        final Instant publicationDateTime = date.toInstant()
                .plus(1, ChronoUnit.MINUTES); // 1 minute up to round up second fraction.

        final Instant now = Instant.now();
        if (publicationDateTime.isBefore(now)) {
            resourceKey = DATE_IN_THE_PAST;
            error(dateIValidatable);
        }
    }

    @Override
    protected Map<String, Object> variablesMap(IValidatable<Date> validatable) {
        final Map<String, Object> map = super.variablesMap(validatable);
        final Date date = validatable.getValue();
        if (date == null) {
            return map;
        }

        final String dateLabel = DateTimePrinter.of(date).print(FormatStyle.LONG, FormatStyle.MEDIUM);
        map.put(INPUTDATE_LABEL, dateLabel);

        return map;
    }

    @Override
    protected String resourceKey() {
        return resourceKey;
    }
}
