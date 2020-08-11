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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.springframework.mock.web.MockRequestDispatcher;

public class HstMockRequestDispatcher extends MockRequestDispatcher {

    protected String url;
    protected HttpServlet servlet;
    
    public HstMockRequestDispatcher(String url, HttpServlet servlet) {
        super(url);
        this.url = url;
        this.servlet = servlet;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public void include(ServletRequest request, ServletResponse response) {
        
        try {
            request.setAttribute("javax.servlet.include_url", this.url);
            servlet.service(request, response);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
}
