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
package org.onehippo.cms7.services.validation.validator;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.validator.RegExpValidator;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class RegExpValidatorTest {

    private ValidatorContext context;
    private RegExpValidator validator;
    private ValidatorConfig config;

    @Before
    public void setUp() {
        config = createMock(ValidatorConfig.class);
        context = createMock(ValidatorContext.class);
    }

    @Test(expected = InvalidValidatorException.class)
    public void throwsExceptionIfRegexpPatternPropertyIsMissing() throws Exception {
        expect(config.hasProperty("regexp.pattern")).andReturn(false);
        expect(config.getName()).andReturn("regexp");
        replayAll();

        validator = new RegExpValidator(config);
    }

    @Test(expected = InvalidValidatorException.class)
    public void throwsExceptionIfFieldIsNotOfTypeString() throws Exception {
        expect(config.hasProperty("regexp.pattern")).andReturn(true);
        expect(config.getProperty("regexp.pattern")).andReturn("[abc]");
        expect(context.getType()).andReturn("not-a-string");
        replayAll();

        validator = new RegExpValidator(config);
        validator.init(context);
    }

    @Test
    public void testValidInput() throws Exception {
        expect(config.hasProperty("regexp.pattern")).andReturn(true);
        expect(config.getProperty("regexp.pattern")).andReturn("[abc]");
        replayAll();

        validator = new RegExpValidator(config);
        assertTrue(validator.isValid(context, "abc"));
        verifyAll();
    }

    @Test
    public void testInvalidInput() throws Exception {
        expect(config.hasProperty("regexp.pattern")).andReturn(true);
        expect(config.getProperty("regexp.pattern")).andReturn("[abc]");
        replayAll();

        validator = new RegExpValidator(config);
        assertFalse(validator.isValid(context, "xyz"));
        verifyAll();
    }

}
