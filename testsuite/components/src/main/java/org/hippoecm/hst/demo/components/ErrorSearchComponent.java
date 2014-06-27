/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.PathUtils;

public class ErrorSearchComponent extends AbstractSearchComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        // set 404 status
        response.setStatus(HstResponse.SC_NOT_FOUND);

        
        String query;
        
        if(request.getParameter("query") != null) {
            query = request.getParameter("query");
        } else {
            String path = PathUtils.normalizePath(request.getPathInfo());
            query = path.substring(path.lastIndexOf('/') + 1);
            if (query == null || "".equals(query)) {
                return;
            }
    
            if (query.endsWith(".html")) {
                query = query.substring(0, query.indexOf(".html"));
            }
        }
        
        doSearch(request, response, query, null, null, DEFAULT_PAGE_SIZE, request.getRequestContext().getSiteContentBaseBean());
        request.setAttribute("isError", Boolean.TRUE);

    }
}
