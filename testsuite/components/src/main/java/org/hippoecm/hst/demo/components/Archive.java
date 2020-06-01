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

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Archive extends AbstractSearchComponent {

    public static final Logger log = LoggerFactory.getLogger(Archive.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        HippoBean currentBean = request.getRequestContext().getContentBean();
        request.setModel("currentBean", currentBean);

        // NOTE: It is intended to not catch NumberFormatException below.
        //       In order to test component exceptions more easily, you can just provide a non-number parameter for 'pageSize'.

        int pageSize = Integer.parseInt(StringUtils.defaultIfEmpty(getPublicRequestParameter(request, "pageSize"), Integer.toString(DEFAULT_PAGE_SIZE)));

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        doSearch(request, response, null, null, "hippostdpubwf:creationDate", pageSize, currentBean);
    }

    /**
     * This example shows how to write a binary output on HST Resource URL request to the client directly
     * without having to attach a hst:resourcetemplate (with servlet or template).
     * In this example, it simply writes a CSV output with 'application/octet-stream' in order to have the download
     * window popped up in browser, for demonstration purpose.
     */
    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        final String resourceId = request.getResourceID();

        if ("download".equals(resourceId)) {
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=\"Document_List.csv\"");
            response.setCharacterEncoding("UTF-8");

            HippoBean currentBean = request.getRequestContext().getContentBean();
            doSearch(request, response, null, null, "hippostdpubwf:creationDate", 1024, currentBean);
            final HstQueryResult result = (HstQueryResult) request.getAttribute("result");

            if (result != null) {
                try (ServletOutputStream out = response.getOutputStream()) {
                    out.println("ID,Name,Path");

                    for (HippoBeanIterator it = result.getHippoBeans(); it.hasNext(); ) {
                        HippoDocumentBean document = (HippoDocumentBean) it.nextHippoBean();
                        out.println(String.format("%s,%s,%s", document.getCanonicalHandleUUID(), document.getName(),
                                document.getCanonicalHandlePath()));
                    }
                } catch (IOException e) {
                    log.error("IO exception occurred.", e);
                }
            }
        }
    }
}