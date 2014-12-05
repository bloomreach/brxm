/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.wicket.mock.MockHomePage;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class RichTextProcessorTest {

    WicketTester tester;

    @Before
    public void createTester() {
        tester = new WicketTester();
        tester.startPage(MockHomePage.class);
    }

    @Test
    public void testExternalLinks() {
        assertExternalLink("/absolute/url/path");
        assertExternalLink("#anchor");
        assertExternalLink("mailto:info@onehippo.com");
        assertExternalLink("callto:skypename");
        assertExternalLink("about:blank");
        assertExternalLink("tel:+310205224466");
        assertExternalLink("funky+scheme-1.0:funkyvalue");
        assertExternalLink("a:value-of-scheme-with-one-letter");
    }

    private void assertExternalLink(final String hrefValue) {
        String text = "<a href=\"" + hrefValue + "\">external link</a>";

        final ILinkDecorator linkDecorator = EasyMock.createMock(ILinkDecorator.class);
        expect(linkDecorator.externalLink(eq(hrefValue))).andReturn("href=\"" + hrefValue + "\"");

        replay(linkDecorator);

        String processed = RichTextProcessor.decorateLinkHrefs(text, linkDecorator);

        assertEquals("<a href=\"" + hrefValue + "\">external link</a>", processed);
        verify(linkDecorator);
    }

}
