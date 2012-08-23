/*
 *  Copyright 2008 - 2011 Hippo.
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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.hst.core.container.HstComponentWindow;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;

/**
 * Factory implementation for creating HTTP Response Wrappers for resource response
 * @version $Id$
 */
public class HstResourceResponseImpl extends HttpServletResponseWrapper implements HstResponse
{
    
    protected HstComponentWindow componentWindow;
    protected String serveResourcePath;

    public HstResourceResponseImpl(HttpServletResponse response, HstComponentWindow componentWindow) {
        super(response);
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
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createRenderURL().");
    }
    
    public HstURL createNavigationalURL(String pathInfo) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createRenderURL().");
    }

    public HstURL createActionURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createActionURL().");
    }

    public HstURL createResourceURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createResourceURL().");
    }

    public HstURL createResourceURL(String referenceNamespace) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createResourceURL().");
    }

    @Override
    public HstURL createComponentRenderingURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createActionURL().");
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

    public void setWrapperElement(Element element) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke setWrapperElement().");
    }

    @Override
    public boolean isRendererSkipped() {
        throw new UnsupportedOperationException("Resource response does not have anything with skipping a renderer.");
    }
    
}
