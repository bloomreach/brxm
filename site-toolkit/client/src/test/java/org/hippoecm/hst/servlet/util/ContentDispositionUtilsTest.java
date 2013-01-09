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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.james.mime4j.codec.DecoderUtil;
import org.hippoecm.hst.servlet.utils.ContentDispositionUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ContentDispositionUtilsTest {

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");
    }

    @Test
    public void testSpecificContentTypes() {
        String pdf = "application/pdf";
        String rtf = "application/rtf";
        String excel = "application/excel";
        String html = "text/html";
        Set<String> types = new HashSet<String>(Arrays.asList(pdf, rtf, excel));

        assertTrue(ContentDispositionUtils.isContentDispositionType(pdf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(rtf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(excel, types));
        assertFalse(ContentDispositionUtils.isContentDispositionType(html, types));
    }

    @Test
    public void testWildcardContentTypes() {
        String pdf = "application/pdf";
        String rtf = "application/rtf";
        String excel = "application/excel";
        String wildcard = "application/*";
        String html = "text/html";
        Set<String> types = new HashSet<String>(Arrays.asList(wildcard));

        assertTrue(ContentDispositionUtils.isContentDispositionType(pdf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(rtf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(excel, types));
        assertFalse(ContentDispositionUtils.isContentDispositionType(html, types));
    }

    @Test
    public void testAllWildcardContentTypes() {
        String pdf = "application/pdf";
        String rtf = "application/rtf";
        String excel = "application/excel";
        String wildcard = "*/*";
        String html = "text/html";
        Set<String> types = new HashSet<String>(Arrays.asList(wildcard));

        assertTrue(ContentDispositionUtils.isContentDispositionType(pdf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(rtf, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(excel, types));
        assertTrue(ContentDispositionUtils.isContentDispositionType(html, types));
    }

    @Test
    public void testAgnosticEncodedFileNames() throws UnsupportedEncodingException {
        String encoding = ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING;

        String fileName = "filename.pdf";
        String expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        fileName = "file name.pdf";
        expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        fileName = "filenam\u00EB.pdf";
        expected = "filename.pdf";
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));
    }

    @Test
    public void testSpecificEncodedFileNames() {
        String encoding = ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING;

        String fileName = "filename.pdf";
        String expected = "filename.pdf";
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        fileName = "file name.pdf";
        expected = "file name.pdf";
        assertEquals(expected, DecoderUtil.decodeEncodedWords(ContentDispositionUtils.encodeFileName(request, response,
                fileName, encoding)));

        fileName = "filenam\u00EB.pdf";
        expected = "filenam\u00EB.pdf";
        assertEquals(expected, DecoderUtil.decodeEncodedWords(ContentDispositionUtils.encodeFileName(request, response,
                fileName, encoding)));
    }

    @Test
    public void testMSIESpecificEncodedFileNames() throws UnsupportedEncodingException {
        String encoding = ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING;
        request.addHeader("User-Agent", "Agent with MSIE in the string");

        String fileName = "filename.pdf";
        String expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        fileName = "file name.pdf";
        expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        // XXX: Is this correct, or should it return just "filename.pdf"?
        fileName = "filenam\u00EB.pdf";
        expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));
    }

    @Test
    public void testOperaSpecificEncodedFileNames() throws UnsupportedEncodingException {
        String encoding = ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING;
        request.addHeader("User-Agent", "Agent with Opera in the string");

        String fileName = "filename.pdf";
        String expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        fileName = "file name.pdf";
        expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));

        // XXX: Is this correct, or should it return just "filename.pdf"?
        fileName = "filenam\u00EB.pdf";
        expected = URLEncoder.encode(fileName, response.getCharacterEncoding());
        assertEquals(expected, ContentDispositionUtils.encodeFileName(request, response, fileName, encoding));
    }

    @Test
    public void testHeaderWithFileName() {
        String encoding = ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING;
        String fileName = "filename.pdf";
        ContentDispositionUtils.addContentDispositionHeader(request, response, fileName, encoding);
        String header = (String) response.getHeader("Content-Disposition");
        assertEquals("attachment; filename=\"filename.pdf\"", header);
    }

    @Test
    public void testHeaderWithoutFileName() {
        String encoding = ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING;
        String fileName = null;
        ContentDispositionUtils.addContentDispositionHeader(request, response, fileName, encoding);
        String header = (String) response.getHeader("Content-Disposition");
        assertEquals("attachment", header);
    }
}
