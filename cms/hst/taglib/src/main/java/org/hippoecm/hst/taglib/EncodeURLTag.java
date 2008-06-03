/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.URLPathTranslator;

/**
 * Tag that appends the result of response.encodeURL to the jsp writer.  
 */
public class EncodeURLTag extends SimpleTagSupport {
    
    private String contextName;
    private String value;
    private String variable;
    
    private URLPathTranslator urlPathTranslator;

    public void setContext(String contextName) {
        this.contextName = contextName;
    }

    public void setVar(String variable) {
        this.variable = variable;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void doTag() throws JspException {

        PageContext pageContext = (PageContext) this.getJspContext(); 
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        
        try {
            String encodedURL;
            
            // normally, use encodeURL as overridden by URLMappingContextFilter
            if (this.contextName == null) {
                encodedURL = response.encodeURL(this.value);
            }
            
            // create a new translator from the given context in the request or pageContext 
            else {
                Context context = (Context) request.getAttribute(this.contextName);
                
                if (context == null) {
                    context = (Context) pageContext.getAttribute(this.contextName);
                }
                
                // need it!
                if (context == null) {
                    throw new JspException("No context found in request or pageContext by attribute name '" + this.contextName + "'.");
                }
                
                // lazy
                if (urlPathTranslator == null) {
                    urlPathTranslator = new URLPathTranslator(context);
                }

                encodedURL = urlPathTranslator.documentPathToURL(this.value);
            }

            // normally, write out
            if (this.variable == null) {
                pageContext.getOut().append(encodedURL);
            }
            
            // ..or set as request attribute if given
            else {
                request.setAttribute(variable, encodedURL);
            }
        } 
        catch (IOException ioe) {
            throw new JspException(ioe);
        }
    }
}
