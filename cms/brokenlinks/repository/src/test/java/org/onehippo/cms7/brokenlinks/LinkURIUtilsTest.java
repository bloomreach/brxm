/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * Tests {@link LinkURIUtils}.
 */
public class LinkURIUtilsTest {

    @Test
    public void testCreateURIFromURLString() throws Exception {
        String url = "http://issues.onehippo.com/browse/[HSTTWO-942]";
        URI uri = LinkURIUtils.createHttpURIFromString(url);
        assertEquals("http://issues.onehippo.com/browse/%5BHSTTWO-942%5D", uri.toString());

        url = "http://localhost:8080/site?q=hello%20world";
        uri = LinkURIUtils.createHttpURIFromString(url);
        assertEquals(url, uri.toString());

        url = "http://www.example.com/?q=hello%20world";
        uri = LinkURIUtils.createHttpURIFromString(url);
        assertEquals(url, uri.toString());

        url = "http://www.example.com/?q=hello%20world#section1";
        uri = LinkURIUtils.createHttpURIFromString(url);
        assertEquals(url, uri.toString());
    }
}
