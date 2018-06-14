/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import net.sf.ehcache.constructs.web.Header;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TestESIPageRenderer
 */
public class TestESIPageRenderer {

    private static final String EXAMPLE_LICENSE_TEXT = "Example License (\"http://www.example.com/LICENSE\")";

    private static Logger log = LoggerFactory.getLogger(TestESIPageRenderer.class);

    @Test
    public void testESIPageRendering() throws Exception {
        ArrayList<Cookie> cookies = new ArrayList<Cookie>();
        cookies.add(new Cookie("id", "571"));
        cookies.add(new Cookie("type", "expat"));
        cookies.add(new Cookie("visits", "42"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "da, en-gb, en");
        request.setCookies(cookies.toArray(new Cookie[cookies.size()]));

        request.setServerName("esi.xyz.com");
        request.addHeader("Referer", "http://roberts.xyz.com/");
        request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:19.0) Gecko/20100101 Firefox/19.0");
        request.setQueryString("first=Robin&last=Roberts");
        request.setParameter("first", "Robin");
        request.setParameter("last", "Roberts");

        int statusCode = HttpServletResponse.SC_OK;
        String contentType = "text/html; charset=UTF-8";
        long timeToLiveSeconds = 60;
        Collection<Header<? extends Serializable>> headers = new ArrayList<Header<? extends Serializable>>();

        byte [] body = readURLContentAsByteArray(getClass().getResource("esi-source-page-1.html"));
        ESIHstPageInfo pageInfo = new ESIHstPageInfo(statusCode, contentType, cookies, body, "UTF-8", timeToLiveSeconds, headers);
        ESIPageScanner scanner = new ESIPageScanner();
        List<ESIFragmentInfo> fragmentInfos = scanner.scanFragmentInfos(pageInfo.getUngzippedBodyAsString());
        pageInfo.addAllFragmentInfos(fragmentInfos);

        StringWriter writer = new StringWriter();

        ESIPageRenderer renderer = new ESIPageRenderer() {
            @Override
            protected void includeRemoteURL(Writer writer, URI uri) {
                if (StringUtils.endsWith(uri.getHost(), "example.com") && "/LICENSE".equals(uri.getPath())) {
                    try {
                        writer.write(EXAMPLE_LICENSE_TEXT);
                    } catch (IOException e) {
                        log.warn("Failed to write content", e);
                    }
                }
            }
        };

        renderer.render(writer, request, pageInfo);

        String expectedBodyContent = new String(readURLContentAsByteArray(getClass().getResource("esi-result-page-1.html")), "UTF-8");
        String renderedBodyContent = writer.toString();
        log.info("renderedBodyContent:\n{}", renderedBodyContent);

        String [] expectedLines = StringUtils.split(expectedBodyContent, "\r\n");
        String [] renderedLines = StringUtils.split(renderedBodyContent, "\r\n");

        for (int i = 0; i < expectedLines.length; i++) {
            assertTrue(renderedLines.length > i);
            assertEquals(expectedLines[i], renderedLines[i]);
        }
    }

    private byte [] readURLContentAsByteArray(URL url) throws IOException {
        InputStream is = null;

        try {
            is = url.openStream();
            return IOUtils.toByteArray(url.openStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
