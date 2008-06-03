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

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.URLMappingResponseWrapper;
import org.hippoecm.hst.core.URLPathTranslator;

public class IncludeTag extends TagSupport {
    
    private static final String KEY_CONTEXT_NAME = "includetag.context.name";
    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final long serialVersionUID = 4078985544852183874L;
    
    private String path;

    public void setPath(String path) {
        this.path = path;
    }

    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        String urlMappingLocation = HSTConfiguration.get(request.getSession().getServletContext(),
                                        HSTConfiguration.KEY_REPOSITORY_URLMAPPING_LOCATION);
        
        String contextName = HSTConfiguration.get(request.getSession().getServletContext(), 
                                        KEY_CONTEXT_NAME, false/*not required*/);
        if (contextName == null) {
            contextName = DEFAULT_CONTEXT_NAME;
        }
        Context context = (Context) request.getAttribute(contextName);
        
        if (context == null) {
            throw new JspException("No context found in request by name '" + contextName + "'.");
        }
        
        try {
            pageContext.getOut().flush();

            // create a new context, used only in the included page
            Context newContext = new Context(context, path);
            request.setAttribute(contextName, newContext);

            URLPathTranslator urlPathTranslator = new URLPathTranslator(newContext);
            URLMappingResponseWrapper responseWrapper = new URLMappingResponseWrapper(newContext, urlPathTranslator, request, response);
            String mappedPage = responseWrapper.mapRepositoryDocument(newContext.getLocation(), urlMappingLocation);
            
            if (mappedPage == null) {
                throw new JspException("No mapped page could be found for location " + newContext.getLocation()
                        + " and url mapping location " + urlMappingLocation);
            }

            // include the page
            RequestDispatcher dispatcher = request.getRequestDispatcher(mappedPage);
            
            if (dispatcher == null) {
                throw new ServletException("No dispatcher could be obtained for mapped page " + mappedPage);
            }
            
            dispatcher.include(request, responseWrapper);
        } 
        catch (ServletException ex) {
            throw new JspException(ex);
        } catch (IOException ex) {
            throw new JspException(ex);
        } catch (RepositoryException ex) {
            throw new JspException(ex);
        } finally {
            // always reset the 'old' context
            request.setAttribute(contextName, context);
        }
        return EVAL_PAGE;
    }
}
