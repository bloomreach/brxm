/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.test.mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.util.HstRequestUtils;

public class HstMockJspServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String myPath = (String) req.getAttribute("javax.servlet.include_url");
        
        StringBuilder content = new StringBuilder().append("JSP output from the page: " + myPath);
        
        PrintWriter out = res.getWriter();
        
        out.println("START:" + content);

        // add children here.

        // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(req);

        if (hstRequest != null) {
            HstComponentWindow myWindow = ((HstRequestImpl) hstRequest).getComponentWindow();
            Map<String, HstComponentWindow> childWindowMap = myWindow.getChildWindowMap();
            
            if (childWindowMap != null) {
                for (String childName : childWindowMap.keySet()) {
                    ((HstResponse) res).flushChildContent(childName);
                }
            }
        }

        out.println("END:" + content);
        out.flush();
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    public void destroy() {
    }

}
