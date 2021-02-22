/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.DOMUtils;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.HeadElementUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HstResponseStateUtils;
import org.hippoecm.hst.util.JsonSerializer;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.hst.util.WrapperElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.hippoecm.hst.util.NullWriter.NULL_WRITER;

/**
 * Temporarily holds the current state of a HST response
 */
public class HstServletResponseState implements HstResponseState {

    private static final Logger log = LoggerFactory.getLogger(HstServletResponseState.class);

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
    protected List<Element> processedElements;
    protected List<Comment> preambleComments;
    protected List<Element> preambleElements;
    protected List<Comment> epilogueComments;
    protected Element wrapperElement;
    protected boolean committed;
    protected boolean hasStatus;
    protected boolean hasError;
    protected Locale locale;
    protected boolean setContentTypeAfterEncoding;
    protected boolean closed;
    protected String characterEncoding;
    protected long contentLength = -1L;
    protected String contentType;
    protected int errorCode;
    protected String errorMessage;
    protected int statusCode;

    protected final HttpServletRequest request;
    protected final HttpServletResponse parentResponse;
    protected final HstComponentWindow window;

    private String redirectLocation;

    private String forwardPathInfo;

    public HstServletResponseState(final HttpServletRequest request,
                                   final HttpServletResponse parentResponse,
                                   final HstComponentWindow window) {
        this.request = request;
        this.parentResponse = parentResponse;
        this.window = window;

        HstRequestContext requestContext = HstRequestUtils.getHstRequestContext(request);

        isActionResponse = (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null);
        isResourceResponse = (requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null);
        isRenderResponse = (!this.isActionResponse && !this.isResourceResponse);

        isStateAwareResponse = isActionResponse;
        isMimeResponse = isRenderResponse || isResourceResponse;
        characterEncoding = parentResponse.getCharacterEncoding();
    }

    public Element createElement(String tagName) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try
        {
            dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
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
            addedHeaders = new HashMap<>();
        }

        List<String> headerList = addedHeaders.get(name);

        if (headerList == null && create) {
            headerList = new ArrayList<>();
            addedHeaders.put(name, headerList);
        }

        return headerList;
    }

    protected List<String> getSetHeaderList(String name, boolean create) {
        if (setHeaders == null) {
            setHeaders = new HashMap<>();
        }

        List<String> headerList = setHeaders.get(name);

        if (headerList == null && create) {
            headerList = new ArrayList<>();
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
                cookies = new ArrayList<>();
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
                        if (contentLength > -1L && byteOutputBuffer.size() >= contentLength) {
                            committed = true;
                            closed = true;
                        }
                    }
                }

                @Override
                public boolean isReady() {
                    // NOTE: The Servlet 3.1 feature that has non-blocking read and write capability is not applicable
                    //       to synchronous HstComponent aggregation. So, this output stream is never ready for non-blocking IO.
                    return false;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    // NOTE: As HST Component aggregation is not required to provide javax.servlet.WriteListener
                    //       of Servlet 3.1 feature to HstComponents, it's unnecessary to implement this.
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
        contentLength = -1L;
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
    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }

    @Override
    public void setContentLengthLong(long len) {
        if (isResourceResponse && !committed && printWriter == null && len > 0L) {
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
                    this.headElements = new ArrayList<>();
                }

                if (element == null) {
                    if (keyHint != null) {
                        KeyValue<String, Element> kvPair = new DefaultKeyValue<>(keyHint, null, true);
                        this.headElements.remove(kvPair);
                    } else {
                        // If element is null and keyHint is null, remove all head elements.
                        this.headElements.clear();
                    }

                    return;
                }

                KeyValue<String, Element> kvPair = new DefaultKeyValue<>(keyHint, element, true);

                if (!this.headElements.contains(kvPair)) {
                    this.headElements.add(kvPair);
                }
            }
        }
    }

    public boolean containsHeadElement(String keyHint) {
        boolean containing = false;

        if (this.headElements != null && keyHint != null) {
            KeyValue<String, Element> kvPair = new DefaultKeyValue<>(keyHint, null, true);
            containing = this.headElements.contains(kvPair);
        }

        return containing;
    }

    @Override
    public void addProcessedHeadElement(final Element headElement) {
        if (parentResponse instanceof HstResponse) {
            ((HstResponse) parentResponse).addProcessedHeadElement(headElement);
        } else {
            if (processedElements == null) {
                processedElements = new ArrayList<>();
            }
            processedElements.add(headElement);
        }
    }

    public List<Element> getHeadElements() {
        List<Element> elements = new LinkedList<>();

        if (this.headElements != null) {
            for (KeyValue<String, Element> kv : this.headElements) {
                elements.add(kv.getValue());
            }
        }

        return elements;
    }

    public void addPreambleNode(Comment comment) {
        if (this.preambleComments == null) {
            this.preambleComments = new ArrayList<>();
        }
        this.preambleComments.add(comment);
    }

    public void addPreambleNode(Element element) {
        if (this.preambleElements == null) {
            this.preambleElements = new ArrayList<>();
        }
        this.preambleElements.add(element);
    }

    public List<Node> getPreambleNodes() {
        if (preambleComments == null && preambleElements == null) {
            return Collections.emptyList();
        }
        List<Node> preambleNodes = new LinkedList<>();
        if (preambleComments != null) {
            preambleNodes.addAll(preambleComments);
        }
        if (preambleElements != null) {
            preambleNodes.addAll(preambleElements);
        }
        return Collections.unmodifiableList(preambleNodes);
    }

    @Override
    public void clearPreambleComments() {
        if (preambleComments == null) {
            return;
        }
        preambleComments.clear();
    }

    @Override
    public void clearEpilogueComments() {
        if (epilogueComments == null) {
            return;
        }
        epilogueComments.clear();
    }

    public void addEpilogueNode(Comment comment) {
        if (epilogueComments == null) {
            epilogueComments = new ArrayList<>();
        }
        epilogueComments.add(comment);
    }

    public List<Node> getEpilogueNodes() {
        if (epilogueComments == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(epilogueComments);
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
        contentLength = -1L;
        contentType = null;
        errorCode = 0;
        errorMessage = null;
        statusCode = 0;
        redirectLocation = null;
    }

    public void flush() throws IOException {
        doFlush(null, false);
    }

    public void flush(Writer writer) throws IOException {
        doFlush(writer, false);
    }

    public void flush(final boolean skipMessageBody) throws IOException {
        doFlush(null, skipMessageBody);
    }

    public void flush(Writer writer, final boolean skipMessageBody) throws IOException {
        doFlush(writer, skipMessageBody);
    }

    private void flushUnflushedChildrenHeaders(final HstComponentWindow hcw, final boolean skipMessageBody) throws IOException {
        if (hcw == null || isActionResponse) {
            return;
        }
        for (String name : hcw.getChildWindowNames()) {
            HstComponentWindow child = hcw.getChildWindow(name);
            if (child == null) {
                continue;
            }
            if (child.isVisible() && child.getResponseState() != null && !child.getResponseState().isFlushed()) {
                log.info("Child window '{}' of window '{}' never got flushed. Flushing its possible present response headers now.",
                        child.getName(), hcw.getName());
                // if this child contains unflushed children, those children will be triggered by child#doFlush
                child.getResponseState().flush(NULL_WRITER, skipMessageBody);
            }
        }
    }

    /**
     * @param writer The writer to write to or {@code null}. The writer MUST be {@code null} when invoked from {@link #flush()}
     *               because if {@code getParentWriter()} gets invoked in {@link #flush()}, the backing http servlet response already
     *               gets committed and a redirect is not possible any more.
     * @param skipMessageBody Whether or not to include the message body or not
     */
    private void doFlush(Writer writer, final boolean skipMessageBody) throws IOException {
        if (flushed) {
            //throw new IllegalStateException("Already flushed");
            // Just ignore...
            return;
        }

        flushUnflushedChildrenHeaders(window, skipMessageBody);

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

            if (isResourceResponse && contentLength > -1L) {
                try {
                    setResponseContentLengthLong(contentLength);
                } catch (UnsupportedOperationException usoe) {
                }
            }

            if (HstRequestUtils.getHstRequestContext(request).isChannelManagerPreviewRequest()) {
                addHeadContributionsReport();
            }

            if (!hasError && redirectLocation == null && !skipMessageBody) {
                if (outputStream != null) {
                    if (!closed) {
                        outputStream.flush();
                    }
                    if (writer == null) {
                        writer = getParentWriter();
                    }
                    int len = byteOutputBuffer.size();
                    if (contentLength > -1L && contentLength < len) {
                        len = (int) contentLength;
                    }

                    printComments(preambleComments, writer);
                    printPreambleElements(preambleElements, writer);

                    if (wrapperElement == null) {
                        if (len > 0) {
                            writer.write(new String(byteOutputBuffer.toByteArray()));
                        }
                    } else {
                        WrapperElement wrapperElem = new WrapperElementImpl(wrapperElement);
                        WrapperElementUtils.writeWrapperElement(writer, wrapperElem, new String(byteOutputBuffer.toByteArray()).toCharArray(), 0, len);
                    }
                    writer.flush();
                    printComments(epilogueComments, writer);
                    outputStream.close();
                    outputStream = null;
                    byteOutputBuffer = null;
                } else if (printWriter != null) {
                    if (!closed) {
                        printWriter.flush();

                        if (writer == null) {
                            writer = getParentWriter();
                        }
                        printComments(preambleComments, writer);
                        printPreambleElements(preambleElements, writer);
                        if (wrapperElement == null) {
                            if (charOutputBuffer.getCount() > 0) {
                                writer.write(charOutputBuffer.getBuffer(), 0, charOutputBuffer.getCount());
                            }
                        } else {
                            WrapperElement wrapperElem = new WrapperElementImpl(wrapperElement);
                            WrapperElementUtils.writeWrapperElement(writer, wrapperElem, charOutputBuffer.getBuffer(), 0, charOutputBuffer.getCount());
                        }
                        writer.flush();
                        printComments(epilogueComments, writer);
                        printWriter.close();

                        printWriter = null;

                        charOutputBuffer = null;
                    }
                } else {
                    if (!closed) {
                        printComments(preambleComments, getParentWriter());
                        printPreambleElements(preambleElements, getParentWriter());
                        printComments(epilogueComments, getParentWriter());
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
     * Writes a list of comments as comment into the output
     * @param comments the list of comments to write
     * @param writer the write to write the preamble elements to
     */
    private void printComments(final List<Comment> comments, final Writer writer) throws IOException {
        if (comments != null) {
            for (Comment comment : comments) {
                HstResponseStateUtils.printComment(comment, writer);
                writer.flush();
            }
        }
    }

    /**
     * Writes the list of preambles elements into the output. Note that only the Element itself and its text gets printed : Not any
     * descendant elements *in* the Element.
     * @param preambles the list of preamble elements to write
     * @param writer the write to write the preamble elements to
     */
    private void printPreambleElements(final List<Element> preambles, final Writer writer) throws IOException {
        if (preambles != null) {
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
        return DOMUtils.createComment(comment);
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

    protected void setResponseStatus(int status) {
        this.parentResponse.setStatus(status);
    }

    protected void setResponseContentLength(int len) {
        setResponseContentLengthLong(len);
    }

    protected void setResponseContentLengthLong(long len) {
        this.parentResponse.setContentLengthLong(len);
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

    private void addHeadContributionsReport() {
        if (headElements != null && !headElements.isEmpty()) {
            final JsonSerializer jsonSerializer = HstServices.getComponentManager().getComponent(JsonSerializer.class);

            // check whether there are head elements that are not included in the response already by
            // a head contributions tag
            final List<String> unprocessed = mapElementsToString(getUnprocessedElementContributions());
            if (!unprocessed.isEmpty()) {
                HeadContributionsReport report = new HeadContributionsReport("HST_UNPROCESSED_HEAD_CONTRIBUTIONS", unprocessed);
                final Comment comment = createComment(jsonSerializer.toJson(report));
                addEpilogueNode(comment);
            }

            final List<String> processed = mapElementsToString(processedElements);
            if (!processed.isEmpty()) {
                HeadContributionsReport report = new HeadContributionsReport("HST_PROCESSED_HEAD_CONTRIBUTIONS", processed);
                final Comment comment = createComment(jsonSerializer.toJson(report));
                addEpilogueNode(comment);
            }

        }
    }

    private List<Element> getUnprocessedElementContributions() {
        return headElements.stream()
                .filter(entry -> processedElements == null || !processedElements.contains(entry.getValue()))
                .map(KeyValue::getValue)
                .collect(Collectors.toList());
    }

    private List<String> mapElementsToString(final List<Element> elements) {
        if (elements == null || elements.isEmpty()) {
            return Collections.emptyList();
        }
        return elements.stream()
                .map(element -> HeadElementUtils.toHtmlString(new HeadElementImpl(element)))
                .collect(Collectors.toList());
    }

    private static class HeadContributionsReport {

        private final String type;
        private final List<String> headElements;

        public HeadContributionsReport(final String type, final List<String> headElements) {
            this.type = type;
            this.headElements = headElements;
        }

        @JsonProperty(ChannelManagerConstants.HST_TYPE)
        public String getType() {
            return type;
        }

        public List<String> getHeadElements() {
            return headElements;
        }
    }
}
