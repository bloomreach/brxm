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

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.hst.core.container.HstComponentWindow;
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

    public void addProperty(String key, Element element) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke addProperty().");
    }

    public Element createElement(String tagName) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createElement().");
    }

    public HstURL createRenderURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createRenderURL().");
    }
    
    public HstURL createActionURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createActionURL().");
    }

    public HstURL createResourceURL() {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke createResourceURL().");
    }

    public String getNamespace() {
        return this.componentWindow.getReferenceNamespace();
    }

    public Map<String, Element> getProperties() {
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
    
    public boolean containsProperty(String key) {
        throw new UnsupportedOperationException("Resource response is not allowed to invoke containsProperty().");
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
    
}
