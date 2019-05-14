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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class RequiredRichTextValidatorTest {

    private RequiredRichTextValidator validator;

    @Before
    public void setUp() {
        validator = new RequiredRichTextValidator();
    }

    @Test(expected = ValidationContextException.class)
    public void validatesOnlyHippoStdHtml() throws Exception {
        final ValidationContext context = createMock(ValidationContext.class);
        final Node node = MockNode.root().addNode("not-hippo-std-html", "nt:unstructured");
        validator.validate(context, node);
    }

    @Test
    public void validInputForHtml() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        replayAll();

        assertValid(validator.validate(context, createContentNode("text")));
        assertValid(validator.validate(context, createContentNode("<p>text</p>")));
        assertValid(validator.validate(context, createContentNode("<img src=\"empty.gif\">")));

        verifyAll();
    }

    @Test
    public void invalidInputForHtml() throws RepositoryException {
        final ValidationContext context = createMock(ValidationContext.class);
        final Violation violation = createMock(Violation.class);

        expect(context.createViolation()).andReturn(violation).times(4);
        replayAll();

        assertInvalid(validator.validate(context, null));
        assertInvalid(validator.validate(context, createContentNode("")));
        assertInvalid(validator.validate(context, createContentNode(" ")));
        assertInvalid(validator.validate(context, createContentNode("<html></html>")));

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

