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
import javax.servlet.ServletException;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.*;

import javax.jcr.RepositoryException;

public class IncludeTag extends TagSupport
{
    private static String SVN_ID = "$Id$";
    private String page;
    private String map;

    public void setPage(String page) {
        this.page = page;
    }
    public void setMap(String map) {
        this.map = map;
    }

    public int doEndTag() throws JspException
    {
        Context newContext, oldContext = (Context) pageContext.findAttribute("context");
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
        boolean success;
        try {
            pageContext.getOut().flush();
            newContext = new Context(oldContext, page, -1);
            request.setAttribute("context", newContext); // should be request.setAttribute("org.hippoecm.hst.context", ..);
            success = RewriteFilter.redirectRepositoryDocument(request, response, newContext, map, page, true);
            if(!success)
                throw new JspException("No document or no document mapping found for "+page);
        } catch(ServletException ex) {
            throw new JspException(ex);
        } catch(IOException ex) {
            throw new JspException(ex);
        } catch(RepositoryException ex) {
            throw new JspException(ex);
        } finally {
            request.setAttribute("context", oldContext); // should be request.setAttribute("org.hippoecm.hst.context", ..);
        }
        return EVAL_PAGE;
    }
}
