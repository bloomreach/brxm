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
package org.hippoecm.hst;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class IncludeTag extends TagSupport {
    private static final long serialVersionUID = 1L;
    
    private String page;
    private String urlMappingLocation;

    public void setPage(String page) {
        this.page = page;
    }

    public void setUrlMappingLocation(String urlMappingLocation) {
        this.urlMappingLocation = urlMappingLocation;
    }

    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        String urlMappingLoc;
        if (this.urlMappingLocation != null) {
            urlMappingLoc = this.urlMappingLocation;
        }
        else {
            urlMappingLoc = (String) request.getSession().getAttribute(ContextFilter.URL_MAPPING_LOCATION);
        }
        
        String contextName = (String) request.getSession().getAttribute(ContextFilter.ATTRIBUTE_NAME);
        Context context = (Context) request.getSession().getAttribute(contextName);
        try {
            pageContext.getOut().flush();

            Context newContext = new Context(context, page, -1);
            request.setAttribute(contextName, newContext);

            URLPathTranslator urlPathTranslator = new URLPathTranslator(request.getContextPath(), 
                                    context.getURLBasePath(), context.getBaseLocation());
            URLMappingResponseWrapper responseWrapper = new URLMappingResponseWrapper(newContext, urlPathTranslator, request, response);
            String mappedPage = responseWrapper.mapRepositoryDocument(context.getLocation(), urlMappingLoc);
            
            if (mappedPage == null) {
                throw new JspException("No mapped page could be found for path " + context.getLocation()
                        + " and urlMappingLocation " + urlMappingLoc);
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
            request.setAttribute(contextName, context);
        }
        return EVAL_PAGE;
    }
}
