/*
 * Copyright 2007-2008 Hippo.
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

import javax.jcr.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.jcr.JCRConnector;

public class ContextTag extends ConditionalTagSupport {
    
    private static final String KEY_CONTEXT_NAME = "contexttag.context.name";
    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final long serialVersionUID = 9184896455255819105L;

    private String contextName;
    private String location;
    private String variable;

    /** Setter for the tag attribute 'var'. */
    public void setVar(String variable) {
        this.variable = variable;
    }

    /** String setter for the tag attribute 'value'. */
    public void setValue(String location) {
        this.location = location;
    }

    /** Context setter for the tag attribute 'value'. */
    public void setValue(Context context) {
        this.location = (context == null ? null : context.getLocation());
    }

    /** Setter for the tag attribute 'context'. */
    public void setContext(String contextName) {
        this.contextName = contextName;
    }

    @Override
    protected boolean condition() throws JspTagException {
        
        if (location == null) {
            return false;
        }    

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        String contextName = getContextName(request);
        Context context = (Context) request.getAttribute(contextName);

        if (context == null) {
            throw new JspTagException("No context found in request by attribute name '" + contextName + "'.");
        }
        
        Context newContext = new Context(context, location);
        if (newContext.exists()) {
            request.setAttribute(variable, newContext);
            return true;
        } else {
            return false;
        }
    }
    
    private String getContextName(HttpServletRequest request) {
        
        // lazy, or (first) set by setter
        if (this.contextName == null) {

            // second by configuration
            this.contextName = HSTConfiguration.get(request.getSession().getServletContext(), 
                    KEY_CONTEXT_NAME, false/*not required*/);
        
            // third by default
            if (this.contextName == null) {
                this.contextName = DEFAULT_CONTEXT_NAME;    
            }
        }
        
        return this.contextName;
    }
}
