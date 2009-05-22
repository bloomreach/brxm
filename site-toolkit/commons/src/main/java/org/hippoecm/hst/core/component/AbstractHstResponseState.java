/*
 *  Copyright 2008 Hippo.
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response
 */
public abstract class AbstractHstResponseState implements HstResponseState {
    
    protected static class CharArrayWriterBuffer extends CharArrayWriter {
        public char[] getBuffer() {
            return buf;
        }

        public int getCount() {
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
    protected Map<String, List<String>> headers;
    protected List<Cookie> cookies;
    protected List<KeyValue<String, Element>> headElements;
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

    private Object response;

    private String redirectLocation;

    public AbstractHstResponseState(Object request, Object response) {
        this.response = response;
        this.defaultLocale = null;
    }

    protected List<String> getHeaderList(String name, boolean create) {
        if (headers == null) {
            headers = new HashMap<String, List<String>>();
        }
        
        List<String> headerList = headers.get(name);
        
        if (headerList == null && create) {
            headerList = new ArrayList<String>();
            headers.put(name, headerList);
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
        addHeader(name, Long.toString(date));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addHeader(java.lang.String,
     * java.lang.String)
     */
    public void addHeader(String name, String value) {
        if (isMimeResponse && !committed) {
            getHeaderList(name, true).add(value);
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
        // Note: Portlet Spec 2.0 demands this to always return false...
        return isMimeResponse && getHeaderList(name, false) != null;
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

            if (isMimeResponse) {
            }
            this.redirectLocation = redirectLocation;
        }
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(String name, long date) {
        setHeader(name, Long.toString(date));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setHeader(java.lang.String,
     * java.lang.String)
     */
    public void setHeader(String name, String value) {
        if (isMimeResponse && !committed) {
            List<String> headerList = getHeaderList(name, true);
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
            this.statusCode = statusCode;
            hasStatus = true;
            resetBuffer();
        }
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
            // Portlet Spec 2.0 requires Portlet Container to supply a "no-op" OutputStream object
            // so delegate back to current PortletServletResponseWrapper to return that one
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
            // Portlet Spec 2.0 requires Portlet Container to supply a "no-op" PrintWriter object
            // so delegate back to current PortletServletResponseWrapper to return that one
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
        headers = null;
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

            if (response instanceof HstResponse) {
                ((HstResponse) response).addHeadElement(element, keyHint);
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

    public void clear() {
        printWriter = null;
        byteOutputBuffer = null;
        charOutputBuffer = null;
        outputStream = null;
        printWriter = null;
        headers = null;
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

            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    for (String value : entry.getValue()) {
                        addResponseHeader(entry.getKey(), value);
                    }
                }
                headers = null;
            }

            if (isResourceResponse && hasStatus) {
                setResponseStatus(statusCode);
            }

            if (isResourceResponse && contentLength > -1) {
                try {
                    setResponseContentLength(contentLength);
                } catch (UnsupportedOperationException usoe) {
                    // TODO: temporary "fix" for JBoss Portal which doesn't yet support this
                    // (although required by the Portlet API 2.0!)
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

                    OutputStream realOutputStream = getResponseOutputStream();
                    int len = byteOutputBuffer.size();
                    if (contentLength > -1 && contentLength < len) {
                        len = contentLength;
                    }
                    if (len > 0) {
                        realOutputStream.write(byteOutputBuffer.toByteArray(), 0, len);
                    }
                    outputStream.close();
                    outputStream = null;
                    byteOutputBuffer = null;
                } else if (printWriter != null) {
                    if (!closed) {
                        printWriter.flush();
                        if (charOutputBuffer.getCount() > 0) {
                            getResponseWriter().write(charOutputBuffer.getBuffer(), 0, charOutputBuffer.getCount());
                        }
                        printWriter.close();

                        printWriter = null;
                        charOutputBuffer = null;
                    }
                }
            }
        }
    }

    protected abstract void setResponseLocale(Locale locale);
    
    protected abstract void addResponseCookie(Cookie cookie);
    
    protected abstract void setResponseCharacterEncoding(String characterEncoding);
    
    protected abstract void setResponseContentType(String contentType);
    
    protected abstract void addResponseHeader(String name, String value);
    
    protected abstract void addResponseHeadElement(Element element, String keyHint);
    
    protected abstract void setResponseStatus(int status);
    
    protected abstract void setResponseContentLength(int len);
    
    protected abstract OutputStream getResponseOutputStream() throws IOException;
    
    protected abstract PrintWriter getResponseWriter() throws IOException;

}
