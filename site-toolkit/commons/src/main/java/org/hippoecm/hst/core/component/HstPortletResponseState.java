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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.ResourceResponse;
import javax.servlet.http.Cookie;

import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response
 */
public class HstPortletResponseState extends AbstractHstResponseState {
    
    protected PortletResponse response;
    protected boolean renderRedirect;

    public HstPortletResponseState(PortletRequest request, PortletResponse response) {
        super(request, response);
        this.response = (PortletResponse) response;

        String lifecyclePhase = (String) request.getAttribute(PortletRequest.LIFECYCLE_PHASE);

        isActionResponse = PortletRequest.ACTION_PHASE.equals(lifecyclePhase);
        isRenderResponse = PortletRequest.RENDER_PHASE.equals(lifecyclePhase);
        isResourceResponse = PortletRequest.RESOURCE_PHASE.equals(lifecyclePhase);
        
        isStateAwareResponse = isActionResponse;
        isMimeResponse = isRenderResponse || isResourceResponse;
    }
    
    public void setRenderRedirect(boolean renderRedirect) {
    	this.renderRedirect = renderRedirect;
    }
    
    public boolean isRenderRedirect() {
    	return renderRedirect;
    }
    
    public Element createElement(String tagName) {
        return this.response.createElement(tagName);
    }

    protected void setResponseLocale(Locale locale) {
        if (isResourceResponse) {
            ((ResourceResponse) this.response).setLocale(locale);
        }
    }

    protected void addResponseCookie(Cookie cookie) {
        this.response.addProperty(cookie);
    }

    protected void setResponseCharacterEncoding(String charset) {
        if (isResourceResponse) {
            ((ResourceResponse) this.response).setCharacterEncoding(charset);
        }
    }

    protected void setResponseContentType(String type) {
        if (isMimeResponse) {
            ((MimeResponse) this.response).setContentType(type);
        }
    }

    protected void addResponseHeader(String name, String value) {
        this.response.addProperty(name, value);
    }
    
    protected void setResponseHeader(String name, String value) {
        this.response.setProperty(name, value);
    }
    
    protected void addResponseHeadElement(Element element, String keyHint) {
        ClassLoader paCL = Thread.currentThread().getContextClassLoader();
        
        try {
            Thread.currentThread().setContextClassLoader(this.response.getClass().getClassLoader());
            this.response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, element);
        } finally {
            Thread.currentThread().setContextClassLoader(paCL);
        }
    }

    protected void setResponseStatus(int status) {
        if (isResourceResponse) {
            ((ResourceResponse) this.response).setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(status));
        }
    }

    protected void setResponseContentLength(int len) {
        if (isResourceResponse) {
            ((ResourceResponse) this.response).setContentLength(len);
        }
    }

    protected OutputStream getResponseOutputStream() throws IOException {
        OutputStream out = null;
        
        if (isMimeResponse) {
            out = ((MimeResponse) this.response).getPortletOutputStream();
        }
        
        return out;
    }

    protected PrintWriter getResponseWriter() throws IOException {
        PrintWriter out = null;
        
        if (isMimeResponse) {
            out = ((MimeResponse) this.response).getWriter();
        }
        
        return out;
    }

}
