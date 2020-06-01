/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.util;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.hippoecm.hst.servlet.utils.HeaderUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HeaderUtilsTest {

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    BinaryPage page;

    @Before
    public void setup() throws UnsupportedEncodingException {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");
        page = new BinaryPage("/content/binaries/my:pdffile");
        page.setLastModified(1234L);
    }

    @Test
    public void testNotForced() {
        assertFalse(HeaderUtils.isForcedCheck(request));
    }

    @Test
    public void testForcedCacheControl() {
        request.addHeader("Cache-Control", "no-cache");
        assertTrue(HeaderUtils.isForcedCheck(request));
    }

    @Test
    public void testForcedPragma() {
        request.addHeader("Pragma", "no-cache");
        assertTrue(HeaderUtils.isForcedCheck(request));
    }

    @Test
    public void testNoETag() {
        assertFalse(HeaderUtils.hasMatchingEtag(request, page));
    }

    @Test
    public void testMatchingETag() {
        request.addHeader("If-None-Match", page.getETag());
        assertTrue(HeaderUtils.hasMatchingEtag(request, page));
    }

    @Test
    public void testNonMatchingETag() {
        request.addHeader("If-None-Match", "non-match");
        assertFalse(HeaderUtils.hasMatchingEtag(request, page));
    }

    @Test
    public void testNoModified() {
        assertTrue(HeaderUtils.isModifiedSince(request, page));
    }

    @Test
    public void testNotModified() {
        request.addHeader("If-Modified-Since", Long.valueOf(1234L));
        assertFalse(HeaderUtils.isModifiedSince(request, page));
    }

    @Test
    public void testModified() {
        request.addHeader("If-Modified-Since", Long.valueOf(1000L));
        assertTrue(HeaderUtils.isModifiedSince(request, page));
    }

    @Test
    public void testSetLastModified() {
        HeaderUtils.setLastModifiedHeaders(response, page);
        String mod = response.getHeader("Last-Modified");
        // header must conform to RFC 1123 date format (please note that milliseconds are lost)
        final SimpleDateFormat rfc1123DateFormat = getRFC1123DateFormat();
        assertEquals(rfc1123DateFormat.format(new Date(1234L)), mod);
    }

    @Test
    public void testSetNoLastModified() {
        page.setLastModified(-1);
        HeaderUtils.setLastModifiedHeaders(response, page);
        Object o = response.getHeader("Last-Modified");
        assertNull(o);
    }

    @Test
    public void testSetExpires() throws ParseException {
        HeaderUtils.setExpiresHeaders(response, page);
        String expires = response.getHeader("Expires");
        final SimpleDateFormat rfc1123DateFormat = getRFC1123DateFormat();


        assertTrue(rfc1123DateFormat.parse(expires).getTime() > System.currentTimeMillis());
        assertNotNull(response.getHeader("Cache-Control"));
    }

    private SimpleDateFormat getRFC1123DateFormat() {
        // header must conform to RFC 1123 date format (please note that milliseconds are lost)
        final SimpleDateFormat rfc1123DateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        rfc1123DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return rfc1123DateFormat;
    }

    @Test
    public void testSetNoExpires() {
        page.setLastModified(-1);
        HeaderUtils.setExpiresHeaders(response, page);
        assertNull(response.getHeader("Expires"));
        assertNull(response.getHeader("Cache-Control"));
    }
}
