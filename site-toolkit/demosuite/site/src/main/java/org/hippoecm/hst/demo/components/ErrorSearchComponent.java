/*
 *  Copyright 2009 Hippo.
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class ErrorSearchComponent extends AbstractSearchComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        // set 404 status
        response.setStatus(HstResponse.SC_NOT_FOUND);

        String query = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        if (query == null || "".equals(query)) {
            return;
        }

        if (query.endsWith(".html")) {
            query = query.substring(0, query.indexOf(".html"));
        }
        
        try {
            query = URLDecoder.decode(query,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not decode requestUri with UTF-8, will pass urlencoded string to query");
        }

        query = query.replace('+', ' ');

        doSearch(request, response, query, null, null, DEFAULT_PAGE_SIZE, getSiteContentBaseBean(request));
        request.setAttribute("isError", Boolean.TRUE);

    }
}
