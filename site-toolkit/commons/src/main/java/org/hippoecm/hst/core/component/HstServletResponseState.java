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
package org.hippoecm.hst.core.component;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.hst.util.WrapperElementUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response
 */
public class HstServletResponseState implements HstResponseState {

    private static class CharArrayWriterBuffer extends CharArrayWriter {
        private char [] getBuffer() {
            return buf;
        }

        private int getCount() {
            return count;
        }
    }

    protected boolean isActionResponse;
    protected boolean isRenderResponse;
    protected boolean isResourceResponse;
    protected boolean isMimeResponse;
    protected boolean isStateAwareResponse;

    protected Locale defaultLocale;
    protected boolean flushed;

    protected ByteArrayOutputStream byteOutputBuffer;
    protected CharArrayWriterBuffer charOutputBuffer;
    protected ServletOutputStream outputStream;
    protected PrintWriter printWriter;
    protected Map<String, List<String>> addedHeaders;
    protected Map<String, List<String>> setHeaders;
    protected List<Cookie> cookies;
    protected List<KeyValue<String, Element>> headElements;
    protected List<Comment> preambleComments;
    protected List<Element> preambleElements;
    protected Element wrapperElement;
    protected boolean committed;
    protected boolean hasStatus;
    protected boolean hasError;
    protected Locale locale;
    protected boolean setContentTypeAfterEncoding;
    protected boolean closed;
    protected String characterEncoding;
    protected int contentLength = -1;
    protected String contentType;
    protected int errorCode;
    protected String errorMessage;
    protected int statusCode;

    protected HttpServletRequest request;
    protected HttpServletResponse parentResponse;


    private String redirectLocation;

    private String forwardPathInfo;

    public HstServletResponseState(HttpServletRequest request, HttpServletResponse parentResponse) {
        this.request = request;
        this.parentResponse = parentResponse;

        HstRequestContext requestContext = HstRequestUtils.getHstRequestContext(request);

        isActionResponse = (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null);
        isResourceResponse = (requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null);
        isRenderResponse = (!this.isActionResponse && !this.isResourceResponse);

        isStateAwareResponse = isActionResponse;
        isMimeResponse = isRenderResponse || isResourceResponse;
    }

    public Element createElement(String tagName) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try
        {
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createElement(tagName);
        }
        catch (ParserConfigurationException e)
        {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

    protected List<String> getAddedHeaderList(String name, boolean create) {
        if (addedHeaders == null) {
            addedHeaders = new HashMap<String, List<String>>();
        }

        List<String> headerList = addedHeaders.get(name);

        if (headerList == null && create) {
            headerList = new ArrayList<String>();
            addedHeaders.put(name, headerList);
        }

        return headerList;
    }

    protected List<String> getSetHeaderList(String name, boolean create) {
        if (setHeaders == null) {
            setHeaders = new HashMap<String, List<String>>();
        }

        List<String> headerList = setHeaders.get(name);

        if (headerList == null && create) {
            headerList = new ArrayList<String>();
            setHeaders.put(name, headerList);
        }

        return headerList;
    }

    protected void failIfCommitted() {
        if (committed) {
            throw new IllegalStateException("Response is already committed");
        }
    }

    public boolean isActionResponse() {
        return isActionResponse;
    }

    public boolean isRenderResponse() {
        return isRenderResponse;
    }

    public boolean isResourceResponse() {
        return isResourceResponse;
    }

    public boolean isMimeResponse() {
        return isMimeResponse;
    }

    public boolean isStateAwareResponse() {
        return isStateAwareResponse;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addCookie(javax.servlet.http.Cookie)
     */
    public void addCookie(Cookie cookie) {
        if (!committed) {
            if (cookies == null) {
                cookies = new ArrayList<Cookie>();
            }
            cookies.add(cookie);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addDateHeader(java.lang.String, long)
     */
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDateHeaderValue(date));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addHeader(java.lang.String,
     * java.lang.String)
     */
    public void addHeader(String name, String value) {
        if (isMimeResponse && !committed) {
            if (statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY &&  "Location".equals(name)) {
                redirectLocation = value;
            }
            getAddedHeaderList(name, true).add(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addIntHeader(java.lang.String, int)
     */
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#containsHeader(java.lang.String)
     */
    public boolean containsHeader(String name) {
        return isMimeResponse && (getAddedHeaderList(name, false) != null || getSetHeaderList(name, false) != null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#sendError(int, java.lang.String)
     */
    public void sendError(int errorCode, String errorMessage) throws IOException {
        failIfCommitted();
        committed = true;
        closed = true;
        hasError = true;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#sendError(int)
     */
    public void sendError(int errorCode) throws IOException {
        sendError(errorCode, null);
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#sendRedirect(java.lang.String)
     */
    public void sendRedirect(String redirectLocation) throws IOException {
        if (isActionResponse || isMimeResponse) {
            failIfCommitted();
            closed = true;
            committed = true;

            this.redirectLocation = redirectLocation;
        }
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    public void forward(String pathInfo) throws IOException {
        if (isRenderResponse) {
            if (parentResponse instanceof HstResponse) {
                ((HstResponse) parentResponse).forward(pathInfo);
            } else {
                failIfCommitted();
                closed = true;
                committed = true;
                forwardPathInfo = pathInfo;
            }
        }
    }

    public String getForwardPathInfo() {
        return forwardPathInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDateHeaderValue(date));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setHeader(java.lang.String,
     * java.lang.String)
     */
    public void setHeader(String name, String value) {
        if (isMimeResponse && !committed) {
            if (statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY &&  "Location".equals(name)) {
                failIfCommitted();
                closed = true;
                committed = true;
                redirectLocation = value;
            }
            List<String> headerList = getSetHeaderList(name, true);
            headerList.clear();
            headerList.add(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setIntHeader(java.lang.String, int)
     */
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setStatus(int, java.lang.String)
     */
    public void setStatus(int statusCode, String message) {
        throw new UnsupportedOperationException("This method is deprecated and no longer supported");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setStatus(int)
     */
    public void setStatus(int statusCode) {
        if (!committed) {
            if (statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY && containsHeader("Location")) {
                // permanent redirect, set #getRedirectLocation to trigger short-circuiting of hst aggregation
                if(setHeaders.get("Location") != null) {
                    failIfCommitted();
                    closed = true;
                    committed = true;
                    redirectLocation = setHeaders.get("Location").get(0);
                } else if (addedHeaders.get("Location") != null) {
                    failIfCommitted();
                    closed = true;
                    committed = true;
                    redirectLocation = addedHeaders.get("Location").get(0);
                }
            }
            if (parentResponse instanceof HstResponse) {
                this.statusCode = statusCode;
                parentResponse.setStatus(statusCode);
            } else {
                this.statusCode = statusCode;
                hasStatus = true;
                resetBuffer();
            }
        }
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        if (isMimeResponse && !closed) {
            committed = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getBufferSize()
     */
    public int getBufferSize() {
        return isMimeResponse ? Integer.MAX_VALUE : 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getCharacterEncoding()
     */
    public String getCharacterEncoding() {
        return isMimeResponse ? characterEncoding != null ? characterEncoding : "ISO-8859-1" : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getContentType()
     */
    public String getContentType() {
        return isMimeResponse ? contentType : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getLocale()
     */
    public Locale getLocale() {
        return isMimeResponse ? locale != null ? locale : defaultLocale : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        if (isStateAwareResponse) {
            return null;
        }
        if (outputStream == null) {
            if (printWriter != null) {
                throw new IllegalStateException("getWriter() has already been called on this response");
            }
            byteOutputBuffer = new ByteArrayOutputStream();
            outputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    if (!closed) {
                        byteOutputBuffer.write(b);
                        if (contentLength > -1 && byteOutputBuffer.size() >= contentLength) {
                            committed = true;
                            closed = true;
                        }
                    }
                }
            };
        }
        return outputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if (isStateAwareResponse) {
            return null;
        }
        if (printWriter == null) {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response");
            }
            charOutputBuffer = new CharArrayWriterBuffer();
            printWriter = new PrintWriter(charOutputBuffer);
        }
        return printWriter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#isCommitted()
     */
    public boolean isCommitted() {
        return isMimeResponse && committed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#reset()
     */
    public void reset() {
        resetBuffer(); // fails if committed
        addedHeaders = null;
        setHeaders = null;
        cookies = null;
        hasStatus = false;
        contentLength = -1;
        if (printWriter == null) {
            contentType = null;
            characterEncoding = null;
            locale = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#resetBuffer()
     */
    public void resetBuffer() {
        failIfCommitted();
        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (Exception e) {
            }
            byteOutputBuffer.reset();
        } else if (printWriter != null) {
            printWriter.flush();
            charOutputBuffer.reset();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        // ignore
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding(String charset) {
        if (isResourceResponse && charset != null && !committed && printWriter == null) {
            characterEncoding = charset;
            setContentTypeAfterEncoding = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#setContentLength(int)
     */
    public void setContentLength(int len) {
        if (isResourceResponse && !committed && printWriter == null && len > 0) {
            contentLength = len;
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (Exception e) {
                }
            }
            if (!closed && byteOutputBuffer != null && byteOutputBuffer.size() >= len) {
                committed = true;
                closed = true;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    public void setContentType(String type) {
        if (isMimeResponse && !committed) {
            contentType = type;
            setContentTypeAfterEncoding = false;
            if (printWriter == null) {
                // TODO: parse possible encoding for better return value from getCharacterEncoding()
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#setLocale(java.util.Locale)
     */
    public void setLocale(Locale locale) {
        if (isResourceResponse && !committed) {
            this.locale = locale;
        }
    }

    public void addHeadElement(Element element, String keyHint) {
        if (isMimeResponse && !committed) {
            // If the component is child of other component, then
            // the property should be passed into the parent component.
            // Otherwise, the property should be kept in the response state.

            if (parentResponse instanceof HstResponse) {
                ((HstResponse) parentResponse).addHeadElement(element, keyHint);
            } else {
                if (this.headElements == null) {
                    this.headElements = new ArrayList<KeyValue<String, Element>>();
                }

                if (element == null) {
                    if (keyHint != null) {
                        KeyValue<String, Element> kvPair = new DefaultKeyValue<String, Element>(keyHint, null, true);
                        this.headElements.remove(kvPair);
                    } else {
                        // If element is null and keyHint is null, remove all head elements.
                        this.headElements.clear();
                    }

                    return;
                }

                KeyValue<String, Element> kvPair = new DefaultKeyValue<String, Element>(keyHint, element, true);

                if (!this.headElements.contains(kvPair)) {
                    this.headElements.add(kvPair);
                }
            }
        }
    }

    public boolean containsHeadElement(String keyHint) {
        boolean containing = false;

        if (this.headElements != null && keyHint != null) {
            KeyValue<String, Element> kvPair = new DefaultKeyValue<String, Element>(keyHint, null, true);
            containing = this.headElements.contains(kvPair);
        }

        return containing;
    }

    public List<Element> getHeadElements() {
        List<Element> elements = new LinkedList<Element>();

        if (this.headElements != null) {
            for (KeyValue<String, Element> kv : this.headElements) {
                elements.add(kv.getValue());
            }
        }

        return elements;
    }

    public void addPreambleNode(Comment comment) {
        if (this.preambleComments == null) {
            this.preambleComments = new ArrayList<Comment>();
        }
        this.preambleComments.add(comment);
    }

    public void addPreambleNode(Element element) {
        if (this.preambleElements == null) {
            this.preambleElements = new ArrayList<Element>();
        }
        this.preambleElements.add(element);
    }

    public void setWrapperElement(Element element) {
        this.wrapperElement = element;
    }

    public Element getWrapperElement() {
        return wrapperElement;
    }

    public void clear() {
        printWriter = null;
        byteOutputBuffer = null;
        charOutputBuffer = null;
        outputStream = null;
        printWriter = null;
        addedHeaders = null;
        setHeaders = null;
        cookies = null;
        committed = false;
        hasStatus = false;
        hasError = false;
        locale = null;
        setContentTypeAfterEncoding = false;
        closed = false;
        characterEncoding = null;
        contentLength = -1;
        contentType = null;
        errorCode = 0;
        errorMessage = null;
        statusCode = 0;
        redirectLocation = null;
    }

    public void flush() throws IOException {
        if (flushed) {
            //throw new IllegalStateException("Already flushed");
            // Just ignore...
            return;
        }

        flushed = true;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                addResponseCookie(cookie);
            }
            cookies = null;
        }

        if (isMimeResponse) {
            if (locale != null) {
                try {
                    setResponseLocale(locale);
                } catch (UnsupportedOperationException usoe) {
                }
            }

            if (contentType != null) {
                if (characterEncoding != null) {
                    if (setContentTypeAfterEncoding) {
                        setResponseCharacterEncoding(characterEncoding);
                        setResponseContentType(contentType);
                    } else {
                        setResponseContentType(contentType);
                        setResponseCharacterEncoding(characterEncoding);
                    }
                } else {
                    setResponseContentType(contentType);
                }
            } else if (characterEncoding != null) {
                setResponseCharacterEncoding(characterEncoding);
            }

            if (addedHeaders != null) {
                for (Map.Entry<String, List<String>> entry : addedHeaders.entrySet()) {
                    for (String value : entry.getValue()) {
                        addResponseHeader(entry.getKey(), value);
                    }
                }
                addedHeaders = null;
            }

            if (setHeaders != null) {
                for (Map.Entry<String, List<String>> entry : setHeaders.entrySet()) {
                    for (String value : entry.getValue()) {
                        setResponseHeader(entry.getKey(), value);
                    }
                }
                setHeaders = null;
            }

            // NOTE: To allow setting status code from each component.
            //if (isResourceResponse && hasStatus) {
            if (hasStatus) {
                setResponseStatus(statusCode);
            }

            if (isResourceResponse && contentLength > -1) {
                try {
                    setResponseContentLength(contentLength);
                } catch (UnsupportedOperationException usoe) {
                }
            }

            if (headElements != null) {
                for (KeyValue<String, Element> entry : headElements) {
                    addResponseHeadElement(entry.getValue(), entry.getKey());
                }

                headElements = null;
            }

            if (!hasError && redirectLocation == null) {
                if (outputStream != null) {
                    if (!closed) {
                        outputStream.flush();
                    }

                    Writer writer = getParentWriter();
                    int len = byteOutputBuffer.size();
                    if (contentLength > -1 && contentLength < len) {
                        len = contentLength;
                    }

                    printPreambleComments(preambleComments);
                    printPreambleElements(preambleElements);

                    if (wrapperElement == null) {
                        if (len > 0) {
                            writer.write(new String(byteOutputBuffer.toByteArray()));
                        }
                    } else {
                        WrapperElement wrapperElem = new WrapperElementImpl(wrapperElement);
                        WrapperElementUtils.writeWrapperElement(writer, wrapperElem, new String(byteOutputBuffer.toByteArray()).toCharArray(), 0, len);
                    }
                    writer.flush();
                    outputStream.close();
                    outputStream = null;
                    byteOutputBuffer = null;
                } else if (printWriter != null) {
                    if (!closed) {
                        printWriter.flush();

                        Writer writer = getParentWriter();
                        printPreambleComments(preambleComments);
                        printPreambleElements(preambleElements);
                        if (wrapperElement == null) {
                            if (charOutputBuffer.getCount() > 0) {
                                writer.write(charOutputBuffer.getBuffer(), 0, charOutputBuffer.getCount());
                            }
                        } else {
                            WrapperElement wrapperElem = new WrapperElementImpl(wrapperElement);
                            WrapperElementUtils.writeWrapperElement(writer, wrapperElem, charOutputBuffer.getBuffer(), 0, charOutputBuffer.getCount());
                        }
                        writer.flush();
                        printWriter.close();

                        printWriter = null;
                        charOutputBuffer = null;
                    }
                } else {
                    if (!closed) {
                        printPreambleComments(preambleComments);
                        printPreambleElements(preambleElements);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFlushed() {
        return flushed;
    }

    /**
     * Writes the list of preambles comments as comment into the output
     * @param preambles the list of preamble comments to write
     */
    private void printPreambleComments(final List<Comment> preambles) throws IOException {
        if (preambles != null) {
            final Writer writer = getParentWriter();
            for (Comment comment : preambles) {
                writer.write("<!-- " + comment.getTextContent() + " -->");
                writer.flush();
            }
        }
    }

    /**
     * Writes the list of preambles elements into the output. Note that only the Element itself and its text gets printed : Not any
     * descendant elements *in* the Element.
     * @param preambles the list of preamble elements to write
     */
    private void printPreambleElements(final List<Element> preambles) throws IOException {
        if (preambles != null) {
            final Writer writer = getParentWriter();
            char[] chars = null;
            int len = 0;
            if (byteOutputBuffer != null) {
                if (characterEncoding != null) {
                    chars = byteOutputBuffer.toString(characterEncoding).toCharArray();
                } else {
                    chars = byteOutputBuffer.toString().toCharArray();
                }
                len = chars.length;
            } else if (charOutputBuffer != null) {
                chars = charOutputBuffer.toCharArray();
                len = chars.length;
            }
            for (Element element : preambles) {
                WrapperElement wrapperElem = new WrapperElementImpl(element);
                WrapperElementUtils.writeWrapperElement(writer, wrapperElem, chars, 0, len);
                writer.flush();
            }
        }
    }

    public Comment createComment(String comment) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createComment(comment);
        } catch (ParserConfigurationException e) {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

    protected void setResponseLocale(Locale locale) {
        this.parentResponse.setLocale(locale);
    }

    protected Writer getParentWriter() throws IOException {
        try {
            return getResponseWriter();
        } catch (IllegalStateException e) {
            final OutputStream realOut = getResponseOutputStream();
            if (characterEncoding != null) {
                return new OutputStreamWriter(realOut, characterEncoding);
            } else {
                return new OutputStreamWriter(realOut);
            }
        }
    }

    protected void addResponseCookie(Cookie cookie) {
        this.parentResponse.addCookie(cookie);
    }

    protected void setResponseCharacterEncoding(String characterEncoding) {
        this.parentResponse.setCharacterEncoding(characterEncoding);
    }

    protected void setResponseContentType(String contentType) {
        this.parentResponse.setContentType(contentType);
    }

    protected void addResponseHeader(String name, String value) {
        this.parentResponse.addHeader(name, value);
    }

    protected void setResponseHeader(String name, String value) {
        this.parentResponse.setHeader(name, value);
    }

    protected void addResponseHeadElement(Element element, String keyHint) {
        if (this.parentResponse instanceof HstResponse) {
            ((HstResponse) this.parentResponse).addHeadElement(element, keyHint);
        }
    }

    protected void setResponseStatus(int status) {
        this.parentResponse.setStatus(status);
    }

    protected void setResponseContentLength(int len) {
        this.parentResponse.setContentLength(len);
    }

    protected OutputStream getResponseOutputStream() throws IOException {
        return this.parentResponse.getOutputStream();
    }

    protected PrintWriter getResponseWriter() throws IOException {
        return this.parentResponse.getWriter();
    }

    private String formatDateHeaderValue(long date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(date));
    }
}
