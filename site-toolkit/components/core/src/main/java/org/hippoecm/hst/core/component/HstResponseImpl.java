/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Factory implementation for creating HTTP Response Wrappers
 * @version $Id$
 */
public class HstResponseImpl extends HttpServletResponseWrapper implements HstResponse
{
    private static final Logger log = LoggerFactory.getLogger(HstResponseImpl.class);
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HstRequestContext requestContext;
    protected HstComponentWindow componentWindow;
    protected HstResponseState responseState;
    protected String redirectLocation;
    protected Map<String, String []> renderParameters;
    protected HstResponse topParentHstResponse;
    protected String renderPath;

    public HstResponseImpl(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final HstRequestContext requestContext,
                           final HstComponentWindow componentWindow,
                           final HstResponse topParentHstResponse) {
        super(response);
        this.request = request;
        this.response = response;
        this.requestContext = requestContext;
        this.componentWindow = componentWindow;
        this.responseState = componentWindow.getResponseState();
        if (responseState == null) {
            throw new IllegalStateException(String.format("HstResponseState for window '%s' is null.", componentWindow.getName()));
        }
        this.topParentHstResponse = topParentHstResponse;
    }
    
    public HstURL createRenderURL() {
        return this.requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, this.componentWindow.getReferenceNamespace(), null, this.requestContext);
    }
    
    public HstURL createNavigationalURL(String pathInfo) {
        HstContainerURL navURL = this.requestContext.getContainerURLProvider().createURL(requestContext.getBaseURL(), pathInfo);
        return this.requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, this.requestContext);
    }

    public HstURL createComponentRenderingURL() {
        return this.requestContext.getURLFactory().createURL(HstURL.COMPONENT_RENDERING_TYPE, this.componentWindow.getReferenceNamespace(), null, this.requestContext);
    }

    public HstURL createActionURL() {
        return this.requestContext.getURLFactory().createURL(HstURL.ACTION_TYPE, this.componentWindow.getReferenceNamespace(), null, this.requestContext);
    }

    public HstURL createResourceURL() {
        return createResourceURL(this.componentWindow.getReferenceNamespace());
    }
    
    public HstURL createResourceURL(String referenceNamespace) {
        return this.requestContext.getURLFactory().createURL(HstURL.RESOURCE_TYPE, referenceNamespace, null,  this.requestContext);
    }
    
    public String getNamespace() {
        return this.componentWindow.getReferenceNamespace();
    }

    public String getRedirectLocation() {
        return this.redirectLocation;
    }
    @Override
    public void addCookie(Cookie cookie)
    {
        responseState.addCookie(cookie);
    }

    @Override
    public void addDateHeader(String name, long date)
    {
        responseState.addDateHeader(name, date);
    }

    @Override
    public void addHeader(String name, String value)
    {
        responseState.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        responseState.addIntHeader(name, value);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return responseState.containsHeader(name);
    }

    @Override
    public void flushBuffer() throws IOException
    {
        responseState.flushBuffer();
    }

    @Override
    public int getBufferSize()
    {
        return responseState.getBufferSize();
    }

    @Override
    public String getCharacterEncoding()
    {
        return responseState.getCharacterEncoding();
    }

    @Override
    public String getContentType()
    {
        return responseState.getContentType();
    }

    @Override
    public Locale getLocale()
    {
        return responseState.getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        ServletOutputStream os = responseState.getOutputStream();
        return os != null ? os : super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        PrintWriter pw = responseState.getWriter();
        return pw != null ? pw : super.getWriter();
    }

    @Override
    public boolean isCommitted()
    {
        return responseState.isCommitted();
    }

    @Override
    public void reset()
    {
        responseState.reset();
    }

    @Override
    public void resetBuffer()
    {
        responseState.resetBuffer();
    }

    @Override
    public void sendError(int errorCode, String errorMessage) throws IOException
    {
        responseState.sendError(errorCode, errorMessage);
    }

    @Override
    public void sendError(int errorCode) throws IOException
    {
        responseState.sendError(errorCode);
    }

    @Override
    public void sendRedirect(String redirectLocation) throws IOException
    {
        responseState.sendRedirect(redirectLocation);
    }

    @Override
    public void setBufferSize(int size)
    {
        responseState.setBufferSize(size);
    }

    public void setCharacterEncoding(String charset)
    {
        responseState.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int len)
    {
        responseState.setContentLength(len);
    }

    @Override
    public void setContentType(String type)
    {
        responseState.setContentType(type);
    }

    @Override
    public void setDateHeader(String name, long date)
    {
        responseState.setDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value)
    {
        responseState.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        responseState.setIntHeader(name, value);
    }

    @Override
    public void setLocale(Locale locale)
    {
        responseState.setLocale(locale);
    }

    @Override
    public void setStatus(int statusCode, String message)
    {
        responseState.setStatus(statusCode, message);
    }

    @Override
    public void setStatus(int statusCode)
    {
        responseState.setStatus(statusCode);
    }

    @Override
    public String encodeRedirectUrl(String url)
    {
        return encodeRedirectURL(url);
    }

    @Override
    public String encodeRedirectURL(String url)
    {
        return url;
    }

    @Override
    public String encodeUrl(String url)
    {
        return encodeURL(url);
    }

    @Override
    public String encodeURL(String url)
    {
        if (url.indexOf("://") == -1 && !url.startsWith("/"))
        {
            // Note: Tomcat does *not* encode the url when called from within a context other
            // than the originating request context (e.g. during cross-context calls) ...
            return url;
        }
        return super.encodeURL(url);
    }

    public Element createElement(String tagName) {
        Element element = null;
        
        if (this.topParentHstResponse != null) {
            element = this.topParentHstResponse.createElement(tagName); 
        } else {
            element = this.responseState.createElement(tagName);
        }
        
        return element;
    }

    public Comment createComment(String comment) {
        Comment element = null;
        if (this.topParentHstResponse != null) {
            element = this.topParentHstResponse.createComment(comment);
        } else {
            element = this.responseState.createComment(comment);
        }
        return element;
    }
    
    public void addHeadElement(Element element, String keyHint) {
        this.responseState.addHeadElement(element, keyHint);
    }
    
    public List<Element> getHeadElements() {
        return this.responseState.getHeadElements();
    }

    public boolean containsHeadElement(String keyHint) {
        boolean contained = false;
        
        if (this.topParentHstResponse == null) {
            contained = this.responseState.containsHeadElement(keyHint);
        } else if (this != this.topParentHstResponse) {
            contained = this.topParentHstResponse.containsHeadElement(keyHint);
        }
        
        return contained;
    }

    @Override
    public void addProcessedHeadElement(final Element headElement) {
        responseState.addProcessedHeadElement(headElement);
    }

    public void addPreamble(Comment comment) {
        responseState.addPreambleNode(comment);
    }

    public void addPreamble(Element element) {
        responseState.addPreambleNode(element);
    }

    @Override
    public List<Node> getPreambleNodes() {
        return responseState.getPreambleNodes();
    }

    public void addEpilogue(Comment comment) {
        responseState.addEpilogueNode(comment);
    }

    @Override
    public List<Node> getEpilogueNodes() {
        return responseState.getEpilogueNodes();
    }

    public void setWrapperElement(Element element) {
        responseState.setWrapperElement(element);
    }
    
    public Element getWrapperElement() {
        return responseState.getWrapperElement();
    }
    
    public void setRenderParameter(String key, String value) {
        if (value == null) {
            setRenderParameter(key, ArrayUtils.EMPTY_STRING_ARRAY);
        } else {
            setRenderParameter(key, new String [] { value });
        }
    }

    public void setRenderParameter(String key, String[] values) {
        if (this.renderParameters == null) {
            this.renderParameters = new HashMap<String, String []>();
        }
        
        if (values == null) {
            this.renderParameters.remove(key);
        } else {
            this.renderParameters.put(key, values);
        }
    }

    public void setRenderParameters(Map<String, String[]> parameters) {
        if (parameters == null) {
            this.renderParameters = null;
        } else {
            if (this.renderParameters == null) {
                this.renderParameters = new HashMap<String, String []>();
            } else {
                this.renderParameters.clear();
            }
        
            for (Map.Entry<String, String []> entry : parameters.entrySet()) {
                setRenderParameter(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public Map<String, String []> getRenderParameters() {
        return this.renderParameters;
    }

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }
    
    public String getRenderPath() {
        return this.renderPath;
    }

    public void setServeResourcePath(String serveResourcePath) {
        throw new UnsupportedOperationException("hst response is not allowed to invoke setServeResourcePath().");
    }

    public void flushChildContent(String name) throws IOException {
        flushChildContent(name, null);
    }

    public void flushChildContent(String name, Writer writer) throws IOException {
        HstComponentWindow childWindow = this.componentWindow.getChildWindow(name);
        if (childWindow == null) {
            log.debug("Cannot find child window with name '{}' for current window '{}'. Skip child.", name, this.componentWindow.getName());
        } else {
            childWindow.getResponseState().flush(writer);
        }
    }
    
    public List<String> getChildContentNames() {
        return this.componentWindow.getChildWindowNames();
    }
    
    public void forward(String pathInfo) throws IOException {
        this.responseState.forward(pathInfo);
    }

    /**
     * A normal {@link HstResponseImpl} never gets its renderer skipped
     */
    @Override
    public boolean isRendererSkipped() {
        return false;
    }

}
