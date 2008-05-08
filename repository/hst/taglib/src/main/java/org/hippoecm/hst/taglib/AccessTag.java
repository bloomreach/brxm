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

public class AccessTag extends ConditionalTagSupport {
    
    private static final String KEY_CONTEXT_NAME = "accesstag.context.name";
    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final long serialVersionUID = 1L;
    
    private String variable;
    private String location;

    public void setVar(String variable) {
        this.variable = variable;
    }

    public void setValue(String location) {
        this.location = location;
    }

    public void setValue(Context location) {
        this.location = (location == null ? null : location.getLocation());
    }

    @Override
    protected boolean condition() throws JspTagException {
        if (location != null) {
            HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();

            String contextName = HSTConfiguration.get(req.getSession().getServletContext(), 
                                                    KEY_CONTEXT_NAME, false/*not required*/);
            if (contextName == null) {
                contextName = DEFAULT_CONTEXT_NAME;
            }
            Context context = (Context) req.getAttribute(contextName);

            Session jcrSession = JCRConnector.getJCRSession(req.getSession());
            Context newContext = new Context(jcrSession, req.getContextPath(), context.getURLBasePath(), context.getBaseLocation());
            context.setRelativeLocation(location);
            if (context.exists()) {
                req.setAttribute(variable, newContext);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
