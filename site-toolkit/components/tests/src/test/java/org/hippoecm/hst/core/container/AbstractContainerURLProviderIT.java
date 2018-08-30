/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.mock.core.container.MockHstComponentWindow;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AbstractContainerURLProviderIT extends AbstractTestConfigurations {

    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    protected HstMutableRequestContext requestContext;

    protected MockHstComponentWindow rootWindow;
    protected MockHstComponentWindow leftChildWindow;
    protected MockHstComponentWindow rightChildWindow;

    @Before
    public void setUp() throws Exception {

        super.setUp();

        this.urlFactory = getComponent(HstURLFactory.class.getName());
        this.requestContext = new HstRequestContextImpl(null);
        this.urlProvider = this.urlFactory.getContainerURLProvider();
        this.requestContext.setURLFactory(urlFactory);

        rootWindow = new MockHstComponentWindow();
        rootWindow.setReferenceName("root");
        rootWindow.setReferenceNamespace("");

        leftChildWindow = new MockHstComponentWindow();
        leftChildWindow.setReferenceName("left");
        leftChildWindow.setReferenceNamespace("l1");
        leftChildWindow.setParentWindow(rootWindow);

        rightChildWindow = new MockHstComponentWindow();
        rightChildWindow.setReferenceName("right");
        rightChildWindow.setReferenceNamespace("r1");
        rightChildWindow.setParentWindow(rootWindow);
    }


    protected HttpServletResponse mockResponse() {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");
        return response;
    }

    protected MockHttpServletRequest mockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setScheme("http");
        request.setServerName("preview.myproject.hippoecm.org");
        request.setServerPort(8080);
        request.setMethod("GET");
        request.setContextPath("/site");
        request.setPathInfo("/news/2008/08");
        request.addHeader("Accept-Language", "da, en-gb, en");
        request.setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        return request;
    }
}
