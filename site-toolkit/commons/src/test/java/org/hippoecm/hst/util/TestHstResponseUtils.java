/*
 *  Copyright 2008 Hippo.
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;

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
        HstLinkCreator linkCreator = createMock(HstLinkCreator.class);
        ResolvedSiteMapItem resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        HstSiteMapItem siteMapItem = createMock(HstSiteMapItem.class);
        HstSiteMap siteMap = createMock(HstSiteMap.class);
        HstSite site = createMock(HstSite.class);
        HstLink link = createMock(HstLink.class);
        
        expect(request.getRequestContext()).andReturn(requestContext);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem);
        expect(siteMapItem.getHstSiteMap()).andReturn(siteMap);
        expect(siteMap.getSite()).andReturn(site);
        expect(requestContext.getHstLinkCreator()).andReturn(linkCreator);
        expect(linkCreator.create(path, site)).andReturn(link);
        expect(link.toUrlForm(request, response, false)).andReturn(expectedUrlWithoutQueryParams);
        response.sendRedirect(expectedUrlWithoutQueryParams);
        
        replay(request);
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(siteMapItem);
        replay(siteMap);
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
        ResolvedSiteMapItem resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        HstSiteMapItem siteMapItem = createMock(HstSiteMapItem.class);
        HstSiteMap siteMap = createMock(HstSiteMap.class);
        HstSite site = createMock(HstSite.class);
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
        
        expect(request.getRequestContext()).andReturn(requestContext);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem);
        expect(siteMapItem.getHstSiteMap()).andReturn(siteMap);
        expect(siteMap.getSite()).andReturn(site);
        expect(requestContext.getHstLinkCreator()).andReturn(linkCreator);
        expect(linkCreator.create(path, site)).andReturn(link);
        expect(link.toUrlForm(request, response, false)).andReturn(expectedUrlWithoutQueryParams);
        expect(response.getCharacterEncoding()).andReturn(null);
        response.sendRedirect(expectedUrl);
        
        replay(request);
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(siteMapItem);
        replay(siteMap);
        replay(linkCreator);
        replay(link);
        replay(response);
        
        HstResponseUtils.sendRedirect(request, response, path, queryParams, characterEncoding);
    }
    
}
