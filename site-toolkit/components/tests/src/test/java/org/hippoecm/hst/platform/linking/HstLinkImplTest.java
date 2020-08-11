/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.util.PathEncoder;
import org.hippoecm.hst.platform.linking.HstLinkImpl;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.hst.util.PathUtils.FULLY_QUALIFIED_URL_PREFIXES;
import static org.junit.Assert.assertEquals;


public class HstLinkImplTest {

    private HstRequestContext context;

    @Before
    public void setup() {
        HstContainerURL containerURL = createNiceMock(HstContainerURL.class);
        context = createNiceMock(HstRequestContext.class);
        expect(context.getBaseURL()).andReturn(containerURL).anyTimes();
        expect(containerURL.getCharacterEncoding()).andReturn("utf-8").anyTimes();
        expect(containerURL.getURIEncoding()).andReturn("utf-8").anyTimes();
        replay(containerURL, context);
    }

    @Test
    public void assert_fully_qualified_url_prefixes_and_encoding() throws UnsupportedEncodingException {

        assertEquals("http://www.onehippo.org/foo", new HstLinkImpl("http://www.onehippo.org/foo", null).toUrlForm(context, false));
        assertEquals("https://www.onehippo.org/foo", new HstLinkImpl("https://www.onehippo.org/foo", null).toUrlForm(context, false));
        assertEquals("//www.onehippo.org/foo", new HstLinkImpl("//www.onehippo.org/foo", null).toUrlForm(context, false));

        // HstLinkImpl removes trailing slash already
        assertEquals("//www.onehippo.org/foo", new HstLinkImpl("//www.onehippo.org/foo/", null).toUrlForm(context, false));
        assertEquals("//www.onehippo.org/foo", new HstLinkImpl("//www.onehippo.org/foo/", null).toUrlForm(context, false));

        // PathEncoder should not remove slashes
        String encoded = PathEncoder.encode("//www.onehippo.org/foo/", "utf-8", FULLY_QUALIFIED_URL_PREFIXES);
        assertEquals("//www.onehippo.org/foo/", encoded);

        encoded = PathEncoder.encode("//www.onehippo.org/foo///", "utf-8", FULLY_QUALIFIED_URL_PREFIXES);
        assertEquals("//www.onehippo.org/foo///", encoded);

        encoded = PathEncoder.encode("//www.onehippo.org/foo///", "utf-8", null);
        assertEquals("//www.onehippo.org/foo///", encoded);
    }


    @Test
    public void assert_xss_attempt_gets_encoded() throws UnsupportedEncodingException {
        final String xssAttempt = "http:a%27%7Dif%281%29%7Balert%28%27owned%27%29-%27";
        final String encoded = new HstLinkImpl("http://www.onehippo.org/" + xssAttempt, null).toUrlForm(context, false);

        final String encodedPart = URLEncoder.encode(xssAttempt, "utf-8");
        assertEquals("http://www.onehippo.org/" + encodedPart, encoded);
    }

}
