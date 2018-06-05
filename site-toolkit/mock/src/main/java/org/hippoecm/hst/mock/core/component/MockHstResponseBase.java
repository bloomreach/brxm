/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.core.component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * MockHstResponseBase
 */
public class MockHstResponseBase implements HttpServletResponse {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private boolean outputStreamUsedByClient;
    private boolean writerUsedByClient;

    private MockServletOutputStream outputStream;
    private PrintWriter writer;

    private String characterEncoding;
    private String contentType;
    private Locale locale;
    private boolean committed;
    private long contentLength;

    private List<Cookie> cookies = new ArrayList<Cookie>();
    private Map<String, List<Object>> headers = new LinkedHashMap<String, List<Object>>();

    private int statusCode;
    private String statusMessage;

    private String redirectLocation;

    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        if (outputStream != null) {
            outputStream.flush();
        }
        committed = true;
    }

    public int getBufferSize() {
        if (outputStream != null) {
            return outputStream.getSize();
        }

        return 0;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getContentLength() {
        return (int) contentLength;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (writerUsedByClient) {
            throw new IllegalStateException("Writer is already used.");
        }

        if (outputStream == null) {
            outputStream = new MockServletOutputStream();
        }

        outputStreamUsedByClient = true;

        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (outputStreamUsedByClient) {
            throw new IllegalStateException("OutputStream is already used.");
        }

        if (writer == null) {
            if (outputStream == null) {
                outputStream = new MockServletOutputStream();
            }

            String encoding = getCharacterEncoding();

            if (encoding == null) {
                encoding = "ISO-8859-1";
            }

            writer = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
        }

        writerUsedByClient = true;

        return writer;
    }

    public byte[] getContentAsByteArray() {
        if (outputStream == null) {
            return EMPTY_BYTE_ARRAY;
        }
        try {
             flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public String getContentAsString() throws UnsupportedEncodingException {
        try {
            flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (characterEncoding != null) {
            return new String(getContentAsByteArray(), characterEncoding);
        }

        return new String(getContentAsByteArray());
    }

    public boolean isCommitted() {
        return committed;
    }

    public void reset() {
        outputStream = null;
        writer = null;
        characterEncoding = null;
        contentType = null;
        locale = null;
        committed = false;
        contentLength = -1L;
        statusCode = 0;
        statusMessage = null;
        redirectLocation = null;
        cookies.clear();
        headers.clear();
        resetBuffer();
    }

    public void resetBuffer() {
        if (committed) {
            throw new IllegalStateException("Already committed.");
        }
        if (outputStream != null) {
            outputStream.reset();
        }
    }

    public void setBufferSize(int size) {
        if (outputStream == null) {
            outputStream = new MockServletOutputStream();
        }
        outputStream.setSize(size);
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public void setContentLengthLong(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public void addDateHeader(String name, long value) {
        addHeaderValue(name, new Date(value));
    }

    public void addHeader(String name, String value) {
        addHeaderValue(name, value);
    }

    public void addIntHeader(String name, int value) {
        addHeaderValue(name, new Integer(value));
    }

    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    public void sendError(int sc) throws IOException {
        if (committed) {
            throw new IllegalStateException("Already committed.");
        }

        committed = true;
        statusCode = sc;
    }

    public void sendError(int sc, String msg) throws IOException {
        sendError(sc);
        statusMessage = msg;
    }

    public void sendRedirect(String location) throws IOException {
        if (committed) {
            throw new IllegalStateException("Already committed.");
        }

        redirectLocation = location;
    }

    public void setDateHeader(String name, long value) {
        headers.remove(name);
        addDateHeader(name, value);
    }

    public void setHeader(String name, String value) {
        headers.remove(name);
        addHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        headers.remove(name);
        addIntHeader(name, value);
    }

    /**
     * @deprecated
     */
    public void setStatus(int sc) {
        setStatus(sc, null);
    }

    public void setStatus(int sc, String msg) {
        statusCode = sc;
        statusMessage = msg;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public String getHeader(final String s) {
        final Collection<String> headers = getHeaders(s);
        if (headers.isEmpty()) {
            return null;
        }
        return headers.iterator().next();
    }

    @Override
    public Collection<String> getHeaders(final String s) {
        final List<Object> headers = this.headers.get(s);
        if (headers == null) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>(headers.size());
        for (Object header : headers) {
            result.add(header.toString());
        }
        return result;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    private void addHeaderValue(String name, Object value) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<Object>();
            headers.put(name, values);
        }
        values.add(value);
    }

    private static class MockServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream internalOutputStream;

        private MockServletOutputStream() {
            internalOutputStream = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            internalOutputStream.write(b);
        }

        @Override
        public void close() throws IOException {
            internalOutputStream.close();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        public void setSize(int size) throws IllegalStateException {
            if (internalOutputStream.size() > 0) {
                throw new IllegalStateException("Already written.");
            }
            internalOutputStream = new ByteArrayOutputStream(size);
        }

        public int getSize() {
            return internalOutputStream.size();
        }

        public void reset() {
            internalOutputStream.reset();
        }

        public byte[] toByteArray() {
            return internalOutputStream.toByteArray();
        }
    }
}
