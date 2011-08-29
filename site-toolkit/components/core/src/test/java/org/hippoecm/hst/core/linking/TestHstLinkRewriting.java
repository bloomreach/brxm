/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.core.linking;


import static junit.framework.Assert.assertEquals;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestHstLinkRewriting extends AbstractBeanTestCase {

        private HstManager hstSitesManager;
        private HstURLFactory hstURLFactory;
        private  ObjectConverter objectConverter;
        private HstLinkCreator linkCreator;

        @Before
        public void setUp() throws Exception {
            super.setUp();

            // Repository repo = getComponent(Repository.class.getName());
            // Credentials cred= getComponent(Credentials.class.getName()+".default");
            this.hstSitesManager = getComponent(HstManager.class.getName());
            this.hstURLFactory = getComponent(HstURLFactory.class.getName());
            this.objectConverter = getObjectConverter();
            this.linkCreator = getComponent(HstLinkCreator.class.getName());;
        }
        
        @After
        public void tearDown() throws Exception {
            super.tearDown();
            
        }

        public HstRequestContext getRequestContextWithResolvedSiteMapItem(String hostAndPort, String requestURI) throws Exception {

            HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
            HstMutableRequestContext requestContext = (HstMutableRequestContext)rcc.create(false);
            HstContainerURL containerUrl = createContainerUrl(hostAndPort, requestURI);
            requestContext.setBaseURL(containerUrl);
            ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
            requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
            HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
            requestContext.setURLFactory(hstURLFactory);
            return requestContext;
        }
        
        public HstContainerURL createContainerUrl(String hostAndPort, String requestURI) throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setServerPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", hostAndPort);
            request.setContextPath("/site");
            requestURI = "/site" + requestURI;
            request.setRequestURI(requestURI);
            VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));
            return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
        }
        
        public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws RepositoryNotAvailableException {
            VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            return vhosts.matchSiteMapItem(url);
        }
     
        @Test
        public void testSimpleHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost:8081/site/home", (homePageLink.toUrlForm(requestContext, true)));
           
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8081/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
        }
        
        /**
         * Linkrewriting with current context is news2 : Now, a link for news2 is expected
         * @throws Exception
         */
        @Ignore
        @Test
        public void testContextAwareHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/news2");

            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","news2/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/news2/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8081/site/news2/News1.html", (newsLink.toUrlForm(requestContext, true)));
     
        }
        
        /**
         * Linkrewriting with current context is newsCtxOnly/news : Now, a link for /newsCtxOnly/news is expected
         * @throws Exception
         */
        @Ignore
        @Test
        public void testContextOnlyHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/newsCtxOnly/foo/news");

            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","newsCtxOnly/foo/news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8081/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
     
            
        }
        
        /**
         * Canonical link never gives a context only link. Since news2 and news are equally suited, the HST will return
         * either a link for /news or for /news2
         * @throws Exception
         */
        @Test
        public void testCanonicalHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/newsCtxOnly/news");
            // TODO add canonical link rewriting here
        }
        
        
        @Test
        public void testExcludedForLinkRewritingSitemapItem() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/news2");
            // TODO add excluded link rewriting here
        }
        
        

        @Test
        public void testCrossDomainHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/news2");
            // TODO add cross domain rewriting here
        }
        

        @Test
        public void testCrossDomainFallbackHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItem("localhost:8081","/news2");
            // TODO add cross domain fallback link rewriting here
        }
        
}
