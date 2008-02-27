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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentMapping extends ContextFilter {
    private String urlmapping;

    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        String param = filterConfig.getInitParameter("urlmapping");
        if (param != null && !param.trim().equals("")) {
            urlmapping = param;
        } else
            throw new ServletException("Missing parameter urlmapping in "+filterConfig.getFilterName());
    }

    public void destroy() {
    }

    @Override
    public boolean doInternal(Context context, String documentPath, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
        try {
            MappingResponseWrapper responseWrapper = new MappingResponseWrapper(context, request, response);
            if (!responseWrapper.redirectRepositoryDocument(urlmapping, documentPath, false)) {
                return false;
            }
        } catch (RepositoryException ex) {
            throw new ServletException(ex);
        }
        return true;
    }
}
