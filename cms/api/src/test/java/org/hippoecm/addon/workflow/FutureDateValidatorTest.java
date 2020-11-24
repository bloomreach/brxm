/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Date;

import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({DateTimePrinter.class, FutureDateValidator.class, Instant.class})
public class FutureDateValidatorTest {

    private static final String FUTURE_DATE_VALIDATOR_CLASS_NAME = FutureDateValidator.class.getSimpleName();

    private FutureDateValidator validator;
    private Instant fixedTimeValue;

    @Before
    public void setUp() {
        // A fixed time value 2016-08-04T11:50:20.100Z is used to mock Instant#now() method
        fixedTimeValue = ZonedDateTime.of(2016, 8, 4, 11, 50, 20, 100, ZoneId.of("Z")).toInstant();
        mockStatic(Instant.class);
        expect(Instant.now()).andReturn(fixedTimeValue);

        // Mock DateTimePrinter.print()
        mockStatic(DateTimePrinter.class);
        final DateTimePrinter mockPrinter = createMock(DateTimePrinter.class);
        expect(DateTimePrinter.of(anyObject(Date.class))).andReturn(mockPrinter);
        expect(mockPrinter.print(eq(FormatStyle.LONG), eq(FormatStyle.MEDIUM))).andReturn("2016-8-4 11:49");

        replayAll();

        validator = new FutureDateValidator();
    }

    @Test
    public void current_datetime_value_should_be_mocked() {
        final Instant now = Instant.now();
        assertThat(now, is(fixedTimeValue));
        PowerMock.verify(Instant.class);
    }

    @Test
    public void valid_if_value_is_in_the_future() {
        final Validatable<Date> validatable = new Validatable<>(createDate(2016, 8, 4, 11, 51));
        validator.validate(validatable);

        PowerMock.verify(Instant.class);
        assertThat(validatable.isValid(), is(true));
    }

    @Test
    public void invalid_if_value_is_null() {
        final Validatable<Date> validatable = new Validatable<>(null);
        validator.validate(validatable);

        assertThat(validatable.isValid(), is(false));
        assertThat(firstError(validatable).getKeys(),
                is(Arrays.asList(FUTURE_DATE_VALIDATOR_CLASS_NAME, FutureDateValidator.EMPTY_DATE)));
    }

    @Test
    public void invalid_if_date_value_is_in_the_past_with_one_second_less() {
        // 2016-08-04T11:49:00Z
        final Validatable<Date> validatable = new Validatable<>(createDate(2016, 8, 4, 11, 49));
        validator.validate(validatable);

        PowerMock.verify(Instant.class);
        assertThat(validatable.isValid(), is(false));
        assertThat(firstError(validatable).getVariables().get(FutureDateValidator.INPUTDATE_LABEL),
                is("2016-8-4 11:49"));
        assertThat(firstError(validatable).getKeys(),
                is(Arrays.asList(FUTURE_DATE_VALIDATOR_CLASS_NAME, FutureDateValidator.DATE_IN_THE_PAST)));
    }

    @Test
    public void invalid_if_date_value_is_current_time_without_second_fraction() {
        // 2016-08-04T11:50:00Z
        final Validatable<Date> validatable = new Validatable<>(createDate(2016, 8, 4, 11, 50));
        validator.validate(validatable);

        PowerMock.verify(Instant.class);
        assertThat(validatable.isValid(), is(true));
    }

    private static ValidationError firstError(final Validatable validatable) {
        return (ValidationError) validatable.getErrors().get(0);
    }

    private static Date createDate(final int year, final int month, final int day, final int hour, final int min) {
        return Date.from(ZonedDateTime.of(year, month, day, hour, min, 0, 0, ZoneId.of("Z")).toInstant());
    }
}