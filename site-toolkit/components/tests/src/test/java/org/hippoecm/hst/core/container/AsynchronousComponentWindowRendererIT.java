/*
 *  Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstURL;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AsynchronousComponentWindowRendererIT extends AbstractContainerURLProviderIT {

    private AbstractAsynchronousComponentWindowRenderer asynchronousComponentWindowRenderer;

    @Before
    public void setUp() throws Exception {
        asynchronousComponentWindowRenderer = new AbstractAsynchronousComponentWindowRenderer() {
            @Override
            public void processWindowBeforeRender(final HstComponentWindow window, final HstRequest request, final HstResponse response) {
                return;
            }
        };
        super.setUp();
    }

    @Test
    public void createAsyncComponentRenderingURL_repeats_current_hstRequest_parameters() {

        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        final String leftWindowPageParam = leftChildWindow.getReferenceNamespace() + ":leftpage";
        final String rightWindowPageParam = rightChildWindow.getReferenceNamespace() + ":rightpage";
        request.setQueryString("foo=bar&"
                + leftWindowPageParam + "=2&"
                + rightWindowPageParam + "=3");

        request.setParameter("foo", "bar");
        request.setParameter(leftWindowPageParam, "2");
        request.setParameter(rightWindowPageParam, "3");

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        final HstResponseState responseState = createNiceMock(HstResponseState.class);
        HstComponentWindow componentLeftWindow = createNiceMock(HstComponentWindow.class);
        expect(componentLeftWindow.getResponseState()).andReturn(responseState);
        expect(componentLeftWindow.getReferenceNamespace()).andReturn(leftChildWindow.getReferenceNamespace()).anyTimes();
        replay(componentLeftWindow);

        HstRequest requestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);

        HstResponseImpl responseLeftWindow = new HstResponseImpl(request, response, requestContext, componentLeftWindow, null);

        {
            final HstURL asyncComponentRenderingLeftChildURL = asynchronousComponentWindowRenderer.createAsyncComponentRenderingURL(requestLeftChildWindow, responseLeftWindow);
            final Map<String, String[]> parameterMap = asyncComponentRenderingLeftChildURL.getParameterMap();
            assertNull("Only current left child component parameters should be on the HstURL", parameterMap.get("r1:rightpage"));
            assertArrayEquals("URL for current component MUST repeat current query params without namespace for AsynchronousComponentWindowRenderer URLs!",
                    new String[]{"2"}, parameterMap.get("leftpage"));
            final String urlString = asyncComponentRenderingLeftChildURL.toString();

            assertTrue(urlString.contains("_hn:type=component-rendering"));
            assertTrue(urlString.contains("_hn:ref=l1"));
            assertTrue(urlString.contains("foo=bar"));
            assertTrue(urlString.contains("r1:rightpage=3"));
            assertTrue(urlString.contains("l1:leftpage=2"));
        }

        {
            HstURL plainComponentRenderingLeftChildURL = responseLeftWindow.createComponentRenderingURL();
            final Map<String, String[]> parameterMap = plainComponentRenderingLeftChildURL.getParameterMap();
            assertFalse("Plain component rendering URLs do *not* repeat parameters for current component",
                    parameterMap.containsKey("leftpage"));

            final String urlString = plainComponentRenderingLeftChildURL.toString();
            assertTrue(urlString.contains("_hn:type=component-rendering"));
            assertTrue(urlString.contains("_hn:ref=l1"));
            assertTrue(urlString.contains("foo=bar"));
            assertTrue(urlString.contains("r1:rightpage=3"));
            assertFalse(urlString.contains("l1:leftpage=2"));
        }

    }
}
