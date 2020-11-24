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

import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorService;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequiredFormattedTextValidatorTest {

    @Test(expected = ValidationContextException.class)
    public void throwsIfHtmlProcessorServiceIsNull() {

        final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
        validator.validate(null, null);
    }

    @Test
    public void validInput() {
        final ValidationContext context = createNiceMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createNiceMock(HtmlProcessorService.class);

        try {
            HippoServiceRegistry.register(htmlProcessorService, HtmlProcessorService.class);

            expect(htmlProcessorService.isVisible("<html></html>")).andReturn(true);
            replay(htmlProcessorService, context);

            final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
            assertFalse(validator.validate(context, "<html></html>").isPresent());
            verify(htmlProcessorService, context);
        } finally {
            HippoServiceRegistry.unregister(htmlProcessorService, HtmlProcessorService.class);
        }
    }


    @Test
    public void invalidInput() {
        final ValidationContext context = createNiceMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createNiceMock(HtmlProcessorService.class);

        try {
            HippoServiceRegistry.register(htmlProcessorService, HtmlProcessorService.class);
            expect(htmlProcessorService.isVisible("<html></html>")).andReturn(false);
            expect(context.createViolation()).andReturn(createNiceMock(Violation.class));
            replay(htmlProcessorService, context);

            final RequiredFormattedTextValidator validator = new RequiredFormattedTextValidator();
            assertTrue(validator.validate(context, "<html></html>").isPresent());
            verify(htmlProcessorService, context);
        } finally {
            HippoServiceRegistry.unregister(htmlProcessorService, HtmlProcessorService.class);
        }

    }
}
