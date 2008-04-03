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
package org.hippoecm.hst;

import javax.jcr.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

public class AccessTag extends ConditionalTagSupport {
    
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

            String attributeName = (String) pageContext.getServletContext().getAttribute(ContextFilter.ATTRIBUTE_NAME);
            Context context = (Context) req.getAttribute(attributeName);

            Session jcrSession = JCRConnector.getJCRSession(req.getSession());
            Context newContext = new Context(jcrSession, context.getURLBasePath(), context.getBaseLocation());
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
