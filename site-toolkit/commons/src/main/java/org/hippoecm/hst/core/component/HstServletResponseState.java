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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response
 */
public class HstServletResponseState extends AbstractHstResponseState
{
    protected HttpServletResponse response;
    
    public HstServletResponseState(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
        this.response = response;
        
        HstRequestContext requestContext = (HstRequestContext) request.getAttribute(HstRequestContext.class.getName());
        
        isActionResponse = (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null);
        isResourceResponse = (requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null);
        isRenderResponse = (!this.isActionResponse && !this.isResourceResponse);
        
        isStateAwareResponse = isActionResponse;
        isMimeResponse = isRenderResponse || isResourceResponse;
    }

    public Element createElement(String tagName) {
        return null;
    }    

    protected void setResponseLocale(Locale locale) {
        this.response.setLocale(locale);
    }
    
    protected void addResponseCookie(Cookie cookie) {
        this.response.addCookie(cookie);
    }
    
    protected void setResponseCharacterEncoding(String characterEncoding) {
        this.response.setCharacterEncoding(characterEncoding);
    }
    
    protected void setResponseContentType(String contentType) {
        this.response.setContentType(contentType);
    }
    
    protected void addResponseHeader(String name, String value) {
        this.response.addHeader(name, value);
    }
    
    protected void addResponseHeadElement(Element element, String keyHint) {
        if (this.response instanceof HstResponse) {
            ((HstResponse) this.response).addHeadElement(element, keyHint);
        }
    }
    
    protected void setResponseStatus(int status) {
        this.response.setStatus(status);
    }
    
    protected void setResponseContentLength(int len) {
        this.response.setContentLength(len);
    }
    
    protected OutputStream getResponseOutputStream() throws IOException {
        return this.response.getOutputStream();
    }
    
    protected PrintWriter getResponseWriter() throws IOException {
        return this.response.getWriter();
    }

}
