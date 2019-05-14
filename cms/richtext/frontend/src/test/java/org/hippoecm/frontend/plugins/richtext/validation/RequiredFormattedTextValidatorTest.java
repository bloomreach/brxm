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
package org.hippoecm.frontend.plugins.richtext.validation;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class RequiredFormattedTextValidatorTest {

    private RequiredFormattedTextValidator validator;

    @Before
    public void setUp() {
        validator = new RequiredFormattedTextValidator();
    }

    @Test
    public void textIsValid() {
        final ValidationContext context = createMock(ValidationContext.class);
        replayAll();

        assertValid(validator.validate(context, "text"));
        verifyAll();
    }

    @Test
    public void paragraphWithTextIsValid() {
        final ValidationContext context = createMock(ValidationContext.class);
        replayAll();

        assertValid(validator.validate(context, "<p>text</p>"));
        verifyAll();
    }

    @Test
    public void imgIsValid() {
        final ValidationContext context = createMock(ValidationContext.class);
        replayAll();

        assertValid(validator.validate(context, "<img src=\"empty.gif\">"));
        verifyAll();
    }

    @Test
    public void nullIsInvalid() {
        final ValidationContext context = createMock(ValidationContext.class);
        expect(context.createViolation()).andReturn(createMock(Violation.class));
        replayAll();

        assertInvalid(validator.validate(context, null));
        verifyAll();
    }

    @Test
    public void blankStringIsInvalid() {
        final ValidationContext context = createMock(ValidationContext.class);
        expect(context.createViolation()).andReturn(createMock(Violation.class)).times(2);
        replayAll();

        assertInvalid(validator.validate(context, ""));
        assertInvalid(validator.validate(context, " "));
        verifyAll();
    }

    @Test
    public void emptyHtmlIsInvalid() {
        final ValidationContext context = createMock(ValidationContext.class);
        expect(context.createViolation()).andReturn(createMock(Violation.class));
        replayAll();

        assertInvalid(validator.validate(context, "<html></html>"));
        verifyAll();
    }

    @Test
    public void emptyParagraphInvalid() {
        final ValidationContext context = createMock(ValidationContext.class);
        expect(context.createViolation()).andReturn(createMock(Violation.class));
        replayAll();

        assertInvalid(validator.validate(context, "<p></p>"));
        verifyAll();
    }

    private static void assertValid(final Optional<Violation> violation) {
        assertFalse(violation.isPresent());
    }

    private static void assertInvalid(final Optional<Violation> violation) {
        assertTrue(violation.isPresent());
    }

}
