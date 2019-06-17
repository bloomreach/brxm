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

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.DateConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SequentialDatesValidatorTest {

    private final static Calendar now, tomorrow, nextWeek, empty;
    private final TestValidationContext context = new TestValidationContext();

    static {
        now = Calendar.getInstance();

        tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        nextWeek = Calendar.getInstance();
        nextWeek.add(Calendar.DAY_OF_YEAR, 7);

        empty = Calendar.getInstance();
        empty.setTime(DateConstants.EMPTY_DATE);
    }

    @Test
    public void testMissingConfiguration() {
        final Node config = MockNode.root();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(SequentialDatesValidator.class).build()) {
            final Validator<Node> validator = new SequentialDatesValidator(config);

            assertFalse(validator.validate(context, null).isPresent());
            assertTrue(interceptor.messages()
                    .anyMatch(m -> m.startsWith("Incorrect configuration of SequentialDatesValidator at")));
        }
    }

    @Test
    public void testInvalidConfiguration() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", ",nonsense,");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(SequentialDatesValidator.class).build()) {
            final Validator<Node> validator = new SequentialDatesValidator(config);

            assertFalse(validator.validate(context, MockNode.root()).isPresent());
            assertTrue(interceptor.messages()
                    .anyMatch(m -> m.startsWith( "Incorrect configuration of SequentialDatesValidator at")));
        }
    }

    @Test
    public void testRightOrder() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate"});

        final Node document = MockNode.root();
        final Calendar now = Calendar.getInstance();
        final Calendar tomorrow = Calendar.getInstance();
        now.setTime(new Date());
        tomorrow.setTime(new Date());
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        document.setProperty("firstDate", now);
        document.setProperty("secondDate", tomorrow);

        final Validator<Node> validator = new SequentialDatesValidator(config);
        assertFalse(validator.validate(context, document).isPresent());
    }

    @Test
    public void testWrongOrder() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate"});

        final Node document = MockNode.root();
        final Calendar now = Calendar.getInstance();
        final Calendar tomorrow = Calendar.getInstance();
        now.setTime(new Date());
        tomorrow.setTime(new Date());
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        document.setProperty("firstDate", tomorrow);
        document.setProperty("secondDate", now);

        final Validator<Node> validator = new SequentialDatesValidator(config);
        assertTrue(validator.validate(context, document).isPresent());
    }

    @Test
    public void testSameDate() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate"});

        final Node document = MockNode.root();
        document.setProperty("firstDate", now);
        document.setProperty("secondDate", now);

        final Validator<Node> validator = new SequentialDatesValidator(config);
        assertTrue(validator.validate(context, document).isPresent());
    }

    @Test
    public void wrongDateFormat() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate"});

        final Node document = MockNode.root();
        document.setProperty("firstDate", "not-a-date");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(SequentialDatesValidator.class).build()) {
            final Validator<Node> validator = new SequentialDatesValidator(config);

            assertFalse(validator.validate(context, document).isPresent());
            assertTrue(interceptor.messages().anyMatch(m -> m.endsWith(
                    "Cannot parse date value")));
        }
    }

    @Test
    public void datePropertiesMissing() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate"});

        final Validator<Node> validator = new SequentialDatesValidator(config);

        assertFalse(validator.validate(context, MockNode.root()).isPresent());
    }

    @Test
    public void moreThanTwoDates() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate", "thirdDate"});

        final Node document = MockNode.root();
        document.setProperty("firstDate", now);
        document.setProperty("secondDate", tomorrow);
        document.setProperty("thirdDate", nextWeek);

        final Validator<Node> validator = new SequentialDatesValidator(config);

        assertFalse(validator.validate(context, document).isPresent());
    }

    @Test
    public void moreThanTwoDatesWrongOrder() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate", "thirdDate"});

        final Node document = MockNode.root();
        document.setProperty("firstDate", now);
        document.setProperty("secondDate", nextWeek);
        document.setProperty("thirdDate", tomorrow);

        final Validator<Node> validator = new SequentialDatesValidator(config);

        assertTrue(validator.validate(context, document).isPresent());
    }

    @Test
    public void skipEmptyDateProperties() throws RepositoryException {
        final Node config = MockNode.root();
        config.setProperty("datePropertyNames", new String[]{"firstDate", "secondDate", "thirdDate"});

        final Node document = MockNode.root();
        document.setProperty("firstDate", now);
        document.setProperty("secondDate", empty);
        document.setProperty("thirdDate", tomorrow);

        final Validator<Node> validator = new SequentialDatesValidator(config);

        assertFalse(validator.validate(context, MockNode.root()).isPresent());
    }
}
