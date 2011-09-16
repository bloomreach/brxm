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
import static junit.framework.Assert.assertTrue;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
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
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestHstLinkRewriting extends AbstractBeanTestCase {

        private HstManager hstSitesManager;
        private HstURLFactory hstURLFactory;
        private  ObjectConverter objectConverter;
        private HstLinkCreator linkCreator;
        private HstSiteMapMatcher siteMapMatcher;

        @Before
        public void setUp() throws Exception {
            super.setUp();

            // Repository repo = getComponent(Repository.class.getName());
            // Credentials cred= getComponent(Credentials.class.getName()+".default");
            this.hstSitesManager = getComponent(HstManager.class.getName());
            this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
            this.hstURLFactory = getComponent(HstURLFactory.class.getName());
            this.objectConverter = getObjectConverter();
            this.linkCreator = getComponent(HstLinkCreator.class.getName());;
        }
        
        @After
        public void tearDown() throws Exception {
            super.tearDown();
            
        }

        
        @Test
        public void testSimpleHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost/site/home", (homePageLink.toUrlForm(requestContext, true)));
           
            
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:80","/home");
            obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            // for absolute links, we do not include port 80 !!
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost/site/home", (homePageLink.toUrlForm(requestContext, true)));
           
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:443","/home");
            obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            // for absolute links, we do not include port 443 !!
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost/site/home", (homePageLink.toUrlForm(requestContext, true)));
           
            
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/home");
            obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            // for absolute links, we do not include port 443 !!
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost:8080/site/home", (homePageLink.toUrlForm(requestContext, true)));
           
            
            
            // on port 8081 we have the preview mount
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8081","/home");
            obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            homePageLink = linkCreator.create((HippoBean)homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be 'home","home", homePageLink.getPath());
            assertEquals("wrong absolute link for homepage" ,"/site/home", (homePageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for homepage" ,"http://localhost:8081/site/home", (homePageLink.toUrlForm(requestContext, true)));
           

            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/home");
            obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
       
            Node handleNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            newsLink = linkCreator.create(handleNode, requestContext);
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
          
            Node docNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1/News1");
            newsLink = linkCreator.create(docNode, requestContext);
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
       
        }
        
        /**
         * the site root content node should return a link to the homepage
         * @throws Exception
         */
        @Test 
        public void testLinkHomePage() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/home");
            Node siteRootContentNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject");
            HstLink homePageLink = linkCreator.create(siteRootContentNode, requestContext);
            assertEquals("wrong link.getPath for /unittestcontent/documents/unittestproject : We expect the homepage for the site content root node ","home", homePageLink.getPath());
        }
        
        @Test 
        public void testLinkNotFound() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/home");
            Node someNode = requestContext.getSession().getNode("/unittestcontent");
            HstLink notFoundLink = linkCreator.create(someNode, requestContext);
            assertEquals("wrong link.getPath for random node that does not belong to site content: Expected was a page not found link","pagenotfound", notFoundLink.getPath());
        }
        
        /**
         * Linkrewriting with current context is alsonews : Now, a link for alsonews/news2 is expected
         * @throws Exception
         */
        @Test
        public void testContextAwareHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/alsonews");

            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","alsonews/news2/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/alsonews/news2/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8080/site/alsonews/news2/News1.html", (newsLink.toUrlForm(requestContext, true)));
     
        }
        
        /**
         * Linkrewriting with current context is newsCtxOnly/news : Now, a link for /newsCtxOnly/news is expected
         * @throws Exception
         */
        @Test
        public void testContextOnlyHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/newsCtxOnly/foo");

            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            assertEquals("wrong link.getPath for News/News1","newsCtxOnly/foo/news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1" ,"/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1" ,"http://localhost:8080/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
     
            
        }
        
        @Test 
        public void testGettingPreferredSiteMapItemIgnoreStartingSlash() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/news");
            HstSiteMapItem sitemapItem1 = requestContext.getSiteMapMatcher().match("/alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
            HstSiteMapItem sitemapItem2 = requestContext.getSiteMapMatcher().match("alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
            assertTrue("We should get the same sitemap item for /alsonews and alsonews but we didn't", sitemapItem1 == sitemapItem2);
        }
        
        /**
         * Even though the context of the current request is /news, we can get a different link then /news by the use 
         * of preferredSitemapItem. Also, the use of fallback & context only concepts are tested.
         * @throws Exception
         */
        @Test
        public void testPreferredSitemapItemHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/news");
            Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
           
            // the preferredSiteMap item is 'alsonews' hence we expect a link to 'alsonews' instead of the 
            // /news/News1.html which we would have gotten normally
            HstSiteMapItem preferSiteMapItem = requestContext.getSiteMapMatcher().match("alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
            HstLink newsLink = linkCreator.create(node , requestContext, preferSiteMapItem, false);
            assertEquals("wrong link.getPath for News/News1","alsonews/news2/News1.html", newsLink.getPath());
            // with or without fallback, we get the same result
            newsLink = linkCreator.create(node , requestContext, preferSiteMapItem, true);
            assertEquals("wrong link.getPath for News/News1","alsonews/news2/News1.html", newsLink.getPath());
            
            // we now set the preferredSiteMap item to a sitemap part that can only a link can be created for if the correct
            // context is injected. The current context is still /news. Hence, the HST will not create a link for the preferred SitemapItem newsCtxOnly/foo but
            // for /news
            preferSiteMapItem = requestContext.getSiteMapMatcher().match("newsCtxOnly/foo", requestContext.getResolvedMount()).getHstSiteMapItem();
            newsLink = linkCreator.create(node , requestContext, preferSiteMapItem, true);
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
            
            // if we now set fallback to false, we will get a not found link because the "newsCtxOnly/foo" sitemap subtree cannot
            // create a link for the item because it misses the context from the resolvedSitemapItem
            newsLink = linkCreator.create(node , requestContext, preferSiteMapItem, false);
            assertEquals("wrong link.getPath for News/News1","pagenotfound", newsLink.getPath());
        
        }
        
        /**
         * Canonical link does not take into account the current context, and never returns a link that can only be created
         * with a context (CtxOnly).
         * 
         * /newsalso/news and /news both are possible to use without a context (opposed to /newsCtxOnly/foo). 
         * Also, the matchers (** relativecontenpath = ${1}) are equally suited. The last check for canonical links, is that if there
         * are two equally suited sitemap items, that the one with the shortest (number of slashes) path is used, thus /news and not /newsonly/news 
         * 
         * @throws Exception
         */
        @Test
        public void testCanonicalHstLinkForBean() throws Exception {
            // current context = /newsCtxOnly
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/newsCtxOnly/news");
            Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong canonical link.getPath for News/News1","news/News1.html", canonicalNewsLink.getPath());

            // current context = /news
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/news");
            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong canonical link.getPath for News/News1","news/News1.html", canonicalNewsLink.getPath());
            
            // current context = /alsonews
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/alsonews");
            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong canonical link.getPath for News/News1","news/News1.html", canonicalNewsLink.getPath());
            
        }

        
        @Test
        public void testLinkBySitemapItemRefId() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/news");
            HstLink homePageLink = linkCreator.createByRefId("homeRefId", requestContext.getResolvedMount().getMount());
            assertEquals("Wrong link for sitemapItemRefId 'homeRefId' ","home", homePageLink.getPath());

            HstLink newsPageLink = linkCreator.createByRefId("newsRefId", requestContext.getResolvedMount().getMount());
            assertEquals("Wrong link for sitemapItemRefId 'newsRefId' ","news", newsPageLink.getPath());
            
            // a refId on a sitemap item that is a wildcard (or one of its ancestors) can not be used to create a link for. It just returns
            // the sitemap item path however
            HstLink wildcardRefIdLink = linkCreator.createByRefId("wildcardNewsRefId", requestContext.getResolvedMount().getMount());
            assertEquals("Wrong link for sitemapItemRefId 'wildcardNewsRefId' ","news/_default_.html", wildcardRefIdLink.getPath());
            
            // non existing refId test
            HstLink nonExistingRefIdLink = linkCreator.createByRefId("nonExistingRefId", requestContext.getResolvedMount().getMount());
            assertTrue("Wrong link for sitemapItemRefId 'wildcardNewsRefId' ",nonExistingRefIdLink == null);
            
        }
            
        @Test
        public void testNavigationStatefulLink() throws Exception {
            
            // test first a preview navigation stateful URL. We need to get the node/bean from the preview context
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8081","/news", "?query=foo&page=6");
            Node node = requestContext.getSession().getNode("/hst:hst/hst:sites/unittestproject-preview/hst:content/News/News1");
            HstLink navigationStatefulNewsLink = linkCreator.create(node, requestContext, null, false, true);
            assertEquals("wrong navigationStateful link.getPath for /hst:hst/hst:sites/unittestproject-preview/hst:content/News/News1","news/News1.html", navigationStatefulNewsLink.getPath());
            
            // a live node should *not* work when current context is preview
            node = requestContext.getSession().getNode("/hst:hst/hst:sites/unittestproject/hst:content/News/News1");
             HstLink brokenNavigationStatefulNewsLink = linkCreator.create(node, requestContext, null, false, true);
            
            assertEquals("wrong navigationStateful link.getPath for /hst:hst/hst:sites/unittestproject/hst:content/News/News1. Because current mount is preview, we cannot get" +
            		"a navigationStateful link of a live node","pagenotfound", brokenNavigationStatefulNewsLink.getPath());
            
            // TODO cannot test now because navigationStateful is baked into HstLinkTag. Should be moved to HstLink#toURLForm
            // TODO see HSTTWO-1786
            //assertEquals("wrong navigationStateful absolute link for /hst:hst/hst:sites/unittestproject-preview/hst:content/News/News1","/site/news/News1.html?query=foo&page=6", navigationStatefulNewsLink.toUrlForm(requestContext, false));
            //assertEquals("wrong navigationStateful fully qualified link for /hst:hst/hst:sites/unittestproject-preview/hst:content/News/News1","http://localhost:8080/site/news/News1.html?query=foo&page=6", navigationStatefulNewsLink.toUrlForm(requestContext, true));

        }
        
        @Test
        public void testExcludedForLinkRewritingSitemapItem() throws Exception {
            // current context points to a location that has a sitemap item that only contains items that are excluded for linkrewriting. Thus, not a link
            // below /newswith_linkrwriting_excluded should be returned
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/newswith_linkrwriting_excluded");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create((HippoBean)newsBean, requestContext);
            // even though current context is /newswith_linkrwriting_excluded and it has sitemap items that could create a link for 
            // the news item, it is not 
            assertEquals("wrong link.getPath for News/News1","news/News1.html", newsLink.getPath());
           
            // Now, show that if we include the 'preferSitemapItem' that is 
            // and at the same time specify fallback is false, that we do get a pagenotfound link, because .. has only items
            // that are specified for linkrewriting = false
            HstSiteMapItem preferSiteMapItem = requestContext.getSiteMapMatcher().match("newswith_linkrwriting_excluded", requestContext.getResolvedMount()).getHstSiteMapItem();
            newsLink = linkCreator.create(((HippoBean)newsBean).getNode() , requestContext, preferSiteMapItem, false);
            assertEquals("wrong link.getPath for News/News1","pagenotfound", newsLink.getPath());
         
        }
        
        

        @Test
        public void testCrossDomainHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:80","/news2");
            // TODO add cross domain rewriting here
        }
        

        @Test
        public void testCrossDomainFallbackHstLinkForBean() throws Exception {
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080","/news2");
            // TODO add cross domain fallback link rewriting here
        }
        
        
        public HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(String hostAndPort, String requestURI) throws Exception {
            return getRequestContextWithResolvedSiteMapItemAndContainerURL(hostAndPort, requestURI, null);
        }
        
        public HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(String hostAndPort, String requestURI, String queryString) throws Exception {
            HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
            HstMutableRequestContext requestContext = (HstMutableRequestContext)rcc.create(false);
            HstContainerURL containerUrl = createContainerUrl(hostAndPort, requestURI, queryString);
            requestContext.setBaseURL(containerUrl);
            ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
            HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
            requestContext.setURLFactory(hstURLFactory);
            requestContext.setSiteMapMatcher(siteMapMatcher);
            return requestContext;
        }
        public HstContainerURL createContainerUrl(String hostAndPort, String requestURI) throws Exception {
            return createContainerUrl(hostAndPort, requestURI, null);
        }
        
        public HstContainerURL createContainerUrl(String hostAndPort, String requestURI, String queryString) throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            String host = hostAndPort.split(":")[0];
            if(hostAndPort.split(":").length > 1) { 
                   int port = Integer.parseInt(hostAndPort.split(":")[1]);
                   request.setLocalPort(port);
                   request.setServerPort(port);
            }
            request.setScheme("http");
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            request.setContextPath("/site");
            request.setQueryString(queryString);
            requestURI = "/site" + requestURI;
            request.setRequestURI(requestURI);
            VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            System.out.println(request.getRequestURL());
            ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));
            return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
        }
        
        public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws RepositoryNotAvailableException {
            VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            return vhosts.matchSiteMapItem(url);
        }
     
        
}
