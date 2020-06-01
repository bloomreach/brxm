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
package org.hippoecm.hst.util;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * TestHstResponseUtils
 * 
 * @version $Id$
 */
public class TestHstResponseUtils {
    
    private String path = "/seeyouagain";
    private String expectedUrlWithoutQueryParams = "/site/preview" + path + ".html";
    private String characterEncoding = "UTF-8";
    
    @Before
    public void setUp() {
        
    }
    
    @Test
    public void testSendRedirect() throws Exception {
        HstRequest request = createMock(HstRequest.class);
        HstResponse response = createMock(HstResponse.class);
        HstRequestContext requestContext = createMock(HstRequestContext.class);
        ResolvedMount resolvedMount = createMock(ResolvedMount.class); 
        Mount mount = createMock(Mount.class); 
        HstLinkCreator linkCreator = createMock(HstLinkCreator.class);
        
        HstLink link = createMock(HstLink.class);
        
        expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();       
        expect(requestContext.getHstLinkCreator()).andReturn(linkCreator).anyTimes();
        expect(linkCreator.create(path, mount)).andReturn(link).anyTimes();
        expect(link.toUrlForm(requestContext, false)).andReturn(expectedUrlWithoutQueryParams).anyTimes();
        response.sendRedirect(expectedUrlWithoutQueryParams);
        
        replay(request);
        replay(requestContext);
        replay(resolvedMount);
        replay(mount);
        replay(linkCreator);
        replay(link);
        replay(response);
        
        HstResponseUtils.sendRedirect(request, response, path);
    }
    
    @Test
    public void testSendRedirectWithQueryParams() throws Exception {
        HstRequest request = createMock(HstRequest.class);
        HstResponse response = createMock(HstResponse.class);
        HstRequestContext requestContext = createMock(HstRequestContext.class);
        HstLinkCreator linkCreator = createMock(HstLinkCreator.class);
        
        ResolvedMount resolvedMount = createMock(ResolvedMount.class); 
        Mount mount = createMock(Mount.class); 
        
        HstLink link = createMock(HstLink.class);
        
        Map<String, String []> queryParams = new HashMap<String, String []>();
        queryParams.put("param1", new String [] { "value 1" });
        queryParams.put("param2", new String [] { "value 2-1", "value 2-2" });
        
        String expectedUrl = expectedUrlWithoutQueryParams + "?";
        for (Map.Entry<String, String []> entry : queryParams.entrySet()) {
            for (String value : entry.getValue()) {
                expectedUrl += entry.getKey() + "=" + URLEncoder.encode(value, characterEncoding) + "&";
            }
        }
        expectedUrl = expectedUrl.substring(0, expectedUrl.length() - 1); // remove ending '&';
        
        expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(requestContext.getHstLinkCreator()).andReturn(linkCreator).anyTimes();
        expect(linkCreator.create(path, mount)).andReturn(link).anyTimes();
        expect(link.toUrlForm(requestContext, false)).andReturn(expectedUrlWithoutQueryParams).anyTimes();
        expect(response.getCharacterEncoding()).andReturn(null).anyTimes();
        response.sendRedirect(expectedUrl);
        
        replay(request);
        replay(requestContext);
        replay(resolvedMount);
        replay(mount);
        replay(linkCreator);
        replay(link);
        replay(response);
        
        HstResponseUtils.sendRedirect(request, response, path, queryParams, characterEncoding);
    }
}
