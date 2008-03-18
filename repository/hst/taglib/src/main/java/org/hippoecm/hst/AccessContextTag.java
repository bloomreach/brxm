/*
 * Copyright 2007 Hippo.
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

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;
import javax.servlet.jsp.jstl.core.LoopTag;
import javax.servlet.jsp.tagext.TagSupport;

public class AccessContextTag extends ConditionalTagSupport {
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
        this.location = (location == null ? null : location.path);
    }

    @Override
    protected boolean condition() throws JspTagException {
        if (location != null) {
            HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
            Session jcrSession = JCRConnector.getJCRSession(req.getSession());
            Context context = new Context(jcrSession, null);
            context.setPath(location);
            if (context.exists()) {
                req.setAttribute(variable, context);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
