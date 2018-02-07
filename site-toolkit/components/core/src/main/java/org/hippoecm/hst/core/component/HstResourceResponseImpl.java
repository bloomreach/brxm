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
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * HTTP Response Wrappers for resource response
 */
public class HstResourceResponseImpl extends HttpServletResponseWrapper implements HstResponse
{

    protected HstRequestContext requestContext;
    protected HstComponentWindow componentWindow;
    protected String serveResourcePath;

    @Deprecated
    public HstResourceResponseImpl(HttpServletResponse response, HstComponentWindow componentWindow) {
        this(response, RequestContextProvider.get(), componentWindow);
    }

    public HstResourceResponseImpl(HttpServletResponse response, HstRequestContext requestContext, HstComponentWindow componentWindow) {
        super(response);
        this.requestContext = requestContext;
        this.componentWindow = componentWindow;
    }

    public void addHeadElement(Element element, String keyHint) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addHeadElement().");
    }

    public Element createElement(String tagName) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createElement().");
    }

    public Comment createComment(String comment) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createComment().");
    }

    public HstURL createRenderURL() {
        return requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, componentWindow.getReferenceNamespace(), null, requestContext);
    }
    
    public HstURL createNavigationalURL(String pathInfo) {
        HstContainerURL navURL = requestContext.getContainerURLProvider().createURL(requestContext.getBaseURL(), pathInfo);
        return requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, requestContext);
    }

    public HstURL createActionURL() {
        return requestContext.getURLFactory().createURL(HstURL.ACTION_TYPE, componentWindow.getReferenceNamespace(), null, requestContext);
    }

    public HstURL createResourceURL() {
        return createResourceURL(componentWindow.getReferenceNamespace());
    }

    public HstURL createResourceURL(String referenceNamespace) {
        return requestContext.getURLFactory().createURL(HstURL.RESOURCE_TYPE, referenceNamespace, null, requestContext);
    }

    @Override
    public HstURL createComponentRenderingURL() {
        return requestContext.getURLFactory().createURL(HstURL.COMPONENT_RENDERING_TYPE, componentWindow.getReferenceNamespace(), null, requestContext);
    }

    public String getNamespace() {
        return this.componentWindow.getReferenceNamespace();
    }

    public List<Element> getHeadElements() {
        return null;
    }

    public void setRenderParameter(String key, String value) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setRenderParameter().");
    }

    public void setRenderParameter(String key, String[] values) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setRenderParameter().");
    }

    public void setRenderParameters(Map<String, String[]> parameters) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setRenderParameters().");
    }
    
    public boolean containsHeadElement(String keyHint) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke containsHeadElement().");
    }

    @Override
    public void addProcessedHeadElement(final Element headElement) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addProcessedHeadElement().");
    }

    public void setRenderPath(String renderPath) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setRenderPath().");
    }

    public void setServeResourcePath(String serveResourcePath) {
        this.serveResourcePath = serveResourcePath;
    }
    
    public String getServeResourcePath() {
        return this.serveResourcePath;
    }
    
    public void flushChildContent(String name) throws IOException {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke flushChildContent().");
    }

    @Override
    public void flushChildContent(final String name, final Writer writer) throws IOException {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke flushChildContent().");
    }

    public List<String> getChildContentNames() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke getChildContentNames().");
    }
    
    public void forward(String pathInfo) throws IOException {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke forward().");
    }

    public Element getWrapperElement() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke getWrapperElement().");
    }

    @Override
    public void addPreamble(final Comment comment) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addPreambleNode().");
    }

    @Override
    public void addPreamble(final Element element) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addPreambleNode().");
    }

    @Override
    public List<Node> getPreambleNodes() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke getPreambleNodes().");
    }

    public void setWrapperElement(Element element) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setWrapperElement().");
    }

    @Override
    public void addEpilogue(final Comment comment) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addEpilogue().");
    }

    @Override
    public List<Node> getEpilogueNodes() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke getEpilogueNodes().");
    }

    @Override
    public boolean isRendererSkipped() {
        throw new UnsupportedOperationException("Resource response does not have anything with skipping a renderer.");
    }
    
}
