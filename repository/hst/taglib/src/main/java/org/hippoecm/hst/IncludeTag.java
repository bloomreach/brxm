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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class IncludeTag extends TagSupport {
    private static final long serialVersionUID = 1L;
    
    private String page;
    private String map;

    public void setPage(String page) {
        this.page = page;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        String contextName = (String) pageContext.findAttribute(RewriteFilter.ATTRIBUTE);
        Context context = (Context) pageContext.findAttribute(contextName);
        try {
            pageContext.getOut().flush();
            Context newContext = new Context(context, page, -1);

            request.setAttribute(contextName, newContext);
            RewriteResponseWrapper responseWrapper = new RewriteResponseWrapper(newContext, request, response);

            if (!responseWrapper.redirectRepositoryDocument(map, page, true)) {
                throw new JspException("No document or no document mapping found for " + page);
            }
        } catch (ServletException ex) {
            throw new JspException(ex);
        } catch (IOException ex) {
            throw new JspException(ex);
        } catch (RepositoryException ex) {
            throw new JspException(ex);
        } finally {
            request.setAttribute(contextName, context);
        }
        return EVAL_PAGE;
    }
}
