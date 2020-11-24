/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorService;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*"})
@PrepareForTest({HippoServiceRegistry.class})
public class RequiredFormattedTextValidatorTest {

    @Before
    public void setUp() {
        mockStatic(HippoServiceRegistry.class);
    }

    @Test(expected = ValidationContextException.class)
    public void throwsIfHtmlProcessorServiceIsNull() {
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(null);
        replayAll();

        final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
        validator.validate(null, null);
    }

    @Test
    public void validInput() {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        expect(htmlProcessorService.isVisible("<html></html>")).andReturn(true);
        replayAll();

        final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
        assertFalse(validator.validate(context, "<html></html>").isPresent());
        verifyAll();
    }

    @Test
    public void invalidInput() {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        expect(htmlProcessorService.isVisible("<html></html>")).andReturn(false);
        expect(context.createViolation()).andReturn(createMock(Violation.class));
        replayAll();

        final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
        Assert.assertTrue(validator.validate(context, "<html></html>").isPresent());
        verifyAll();
    }
}
