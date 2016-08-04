/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.addon.workflow;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.Validatable;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FutureDateValidatorTest {

    // Fixed the time zone to test
    final ZoneOffset zoneOffset = ZoneOffset.of("+2");
    final ZoneId currentZone = ZoneId.of(zoneOffset.getId());

    private FutureDateValidator validator;

    @Before
    public void setUp() {
        // Fixed clock to a specific time 2016-08-04T11:50:20.100+02
        final Clock fixedClock = Clock.fixed(LocalDateTime.of(2016, 8, 4, 11, 50, 20, 100).toInstant(zoneOffset), currentZone);

        validator = new FutureDateValidator(fixedClock) {
            @Override
            protected Map<String, Object> variablesMap(final IValidatable<Date> validatable) {
                // Mock this method
                return Collections.emptyMap();
            }
        };
    }

    @Test
    public void valid_if_value_is_in_the_future() {
        final Validatable validatable = new Validatable(from(LocalDateTime.of(2016, 8, 4, 11, 51), currentZone));
        validator.onValidate(validatable);
        assertThat(validatable.isValid(), is(true));
    }

    @Test
    public void invalid_if_value_is_null() {
        final Validatable validatable = new Validatable(null);
        validator.onValidate(validatable);

        assertThat(validatable.isValid(), is(false));
        assertThat(validator.resourceKey(), is(FutureDateValidator.EMPTY_DATE));
    }

    @Test
    public void invalid_if_date_value_is_in_the_past_with_one_second_less() {
        // 2016-08-04T11:49:00+02
        final Validatable validatable = new Validatable(from(LocalDateTime.of(2016, 8, 4, 11, 49), currentZone));
        validator.onValidate(validatable);

        assertThat(validatable.isValid(), is(false));
        assertThat(validator.resourceKey(), is(FutureDateValidator.DATE_IN_THE_PAST));
    }

    @Test
    public void invalid_if_date_value_is_current_time_without_second_fraction() {
        // 2016-08-04T11:50:00+02
        final Validatable validatable = new Validatable(from(LocalDateTime.of(2016, 8, 4, 11, 50), currentZone));
        validator.onValidate(validatable);

        assertThat(validatable.isValid(), is(true));
    }

    private static Date from(LocalDateTime localDateTime, ZoneId zoneId) {
        return Date.from(localDateTime.atZone(zoneId).toInstant());
    }
}