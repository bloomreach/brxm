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

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorService;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({HippoServiceRegistry.class})
public class RequiredRichTextValidatorTest {

    @Before
    public void setUp() {
        mockStatic(HippoServiceRegistry.class);
    }

    @Test(expected = ValidationContextException.class)
    public void throwsIfHtmlProcessorServiceIsNull() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(null);
        replayAll();

        final RequiredRichTextValidator validator = new RequiredRichTextValidator();
        validator.validate(context, createContentNode("<html></html>"));
    }

    @Test(expected = ValidationContextException.class)
    public void validatesOnlyHippoStdHtml() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        final Node node = MockNode.root().addNode("not-hippo-std-html", "nt:unstructured");
        replayAll();

        final RequiredRichTextValidator validator = new RequiredRichTextValidator();
        validator.validate(context, node);
    }

    @Test
    public void validInput() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        expect(htmlProcessorService.isVisible("<html></html>")).andReturn(true);
        replayAll();

        final RequiredRichTextValidator validator = new RequiredRichTextValidator();
        assertValid(validator.validate(context, createContentNode("<html></html>")));

        verifyAll();
    }

    @Test
    public void invalidInput() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        expect(htmlProcessorService.isVisible("<html></html>")).andReturn(false);
        expect(context.createViolation()).andReturn(createMock(Violation.class));
        replayAll();

        final RequiredRichTextValidator validator = new RequiredRichTextValidator();
        assertInvalid(validator.validate(context, createContentNode("<html></html>")));

        verifyAll();
    }

    @Test
    public void nullIsInvalid() {
        final ValidationContext context = createMock(ValidationContext.class);
        final HtmlProcessorService htmlProcessorService = createMock(HtmlProcessorService.class);
        expect(HippoServiceRegistry.getService(HtmlProcessorService.class)).andReturn(htmlProcessorService);
        final Violation violation = createMock(Violation.class);
        expect(context.createViolation()).andReturn(violation);
        replayAll();

        final RequiredRichTextValidator validator = new RequiredRichTextValidator();
        assertInvalid(validator.validate(context, null));

        verifyAll();
    }

    private static void assertValid(final Optional<Violation> violation) {
        assertFalse(violation.isPresent());
    }

    private static void assertInvalid(final Optional<Violation> violation) {
        assertTrue(violation.isPresent());
    }

    private static Node createContentNode(final String value) throws RepositoryException {
        final Node contentNode = MockNode.root().addNode("content", HippoStdNodeType.NT_HTML);
        contentNode.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, value);
        return contentNode;
    }
}
