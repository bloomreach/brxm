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
package org.hippoecm.hst.configuration;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestMatchHostAndURL extends AbstractSpringTestCase {

        private VirtualHostsManager virtualHostsManager;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            this.virtualHostsManager = getComponent(VirtualHostsManager.class.getName());
        }

     
        @Test
        public void testDefaultHost(){
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                assertTrue("Expected from the hst testcontents default hostname to be 127.0.0.1. ", "127.0.0.1".equals(vhosts.getDefaultHostName()));
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        
        /*
         *  we configured the following hosts for test content:
         *  
         *  127.0.0.1
         *  localhost
         *  test
         *    ` onehippo
         *            |- www
         *            `- preview
         *          
         */
        @Test
        public void testAllHosts(){
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                List<VirtualHost> allHosts = vhosts.getVirtualHosts(false);
               
                /*
                 * we expect the following hosts now:
                 * 1
                 * 0.1
                 * 0.0.1
                 * 127.0.0.1
                 * localhost
                 * test
                 * onehippo.test
                 * www.onehippo.test
                 * preview.onehippo.test
                 */
                
                 assertTrue("We expect 9 hosts in total", allHosts.size() == 9);
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        /*
         * From all the hosts above, actually only the host 127.0.0.1, localhost, www.onehippo.org and preview.onehippo.org do have 
         * a SiteMount. Thus the number of mounted hosts should be 4
         */
        @Test
        public void testMountedHosts(){
            
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                List<VirtualHost> mountedHosts = vhosts.getVirtualHosts(true);
                assertTrue("We expect 4 mounted hosts in total but found '"+mountedHosts.size()+"' hosts", mountedHosts.size() == 4);
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing SiteMount should be live
         */
        @Test
        public void testMatchNoContextPath(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setRequestURI("/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the pathInfo from the requestURI did not match the preview SiteMount, so our siteMount must be live:
                assertFalse("We should have a match in LIVE ",resolvedSiteMapItem.getResolvedSiteMount().getSiteMount().isPreview());
                assertTrue("The live SiteMount must have an empty string \"\" as pathInfoPrefix", "".equals(resolvedSiteMapItem.getResolvedSiteMount().getResolvedPathInfoPrefix()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing SiteMount should be preview
         */
        @Test
        public void testMatchPreviewNoContextPath(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setRequestURI("/preview/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the pathInfo from the requestURI should match the preview SiteMount, so our siteMount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedSiteMount().getSiteMount().isPreview());
                assertTrue("The preview SiteMount must have '/preview' as pathInfoPrefix", "/preview".equals(resolvedSiteMapItem.getResolvedSiteMount().getResolvedPathInfoPrefix()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest *does* have a context path
         * The backing SiteMount should be live
         */
        @Test
        public void testMatchWithContextPath(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setContextPath("/site");
            request.setRequestURI("/site/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                assertFalse("We should have a match in LIVE ",resolvedSiteMapItem.getResolvedSiteMount().getSiteMount().isPreview());
                assertTrue("The live SiteMount must have an empty string \"\" as pathInfoPrefix", "".equals(resolvedSiteMapItem.getResolvedSiteMount().getResolvedPathInfoPrefix()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing SiteMount should be preview
         */
        @Test
        public void testMatchPreviewWithContextPath(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setContextPath("/site");
            request.setRequestURI("/site/preview/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the pathInfo from the requestURI should match the preview SiteMount, so our siteMount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedSiteMount().getSiteMount().isPreview());
                assertTrue("The preview SiteMount must have '/preview' as pathInfoPrefix", "/preview".equals(resolvedSiteMapItem.getResolvedSiteMount().getResolvedPathInfoPrefix()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing SiteMount should be 'preview/services'
         * This SiteMount is configured with a namedPipeline = 'JaxrsPipeline', hence, the resolvedSiteMapItem.getNamedPipeline should return 'JaxrsPipeline'
         * The SiteMount 'preview/services' should inherit the mountPath from its parent, which is 'preview'
         */
        @Test
        public void testMatchPreviewServices(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setRequestURI("/preview/services/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the pathInfo from the requestURI should match the preview SiteMount, so our siteMount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedSiteMount().getSiteMount().isPreview());
                assertTrue("The preview SiteMount must have '/preview/services' as pathInfoPrefix", "/preview/services".equals(resolvedSiteMapItem.getResolvedSiteMount().getResolvedPathInfoPrefix()));
                
                // because the /preview/services SiteMount has configured a different pipeline, the resolvedSiteMapItem should reflect this:
                assertTrue("Expected pipeline name is 'JaxrsPipeline' ", "JaxrsPipeline".equals(resolvedSiteMapItem.getNamedPipeline()));
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * We now make a request uri to 
         * /preview/services/pipelines/custom   and
         * /preview/services/pipelines/general  where the custom has a specific pipeline configured on the matching sitemap item and
         * general should fallback to the pipeline configured on the sitemount '/preview/services'
         */
        @Test
        public void testSiteMapItemNamedPipeline(){
            MockHttpServletRequest requestCustom = new MockHttpServletRequest();
            requestCustom.setLocalPort(8081);
            requestCustom.setScheme("http");
            requestCustom.setServerName("127.0.0.1");
            requestCustom.setRequestURI("/preview/services/pipelines/custom");

            MockHttpServletRequest requestGeneral = new MockHttpServletRequest();
            requestGeneral.setLocalPort(8081);
            requestGeneral.setScheme("http");
            requestGeneral.setServerName("127.0.0.1");
            requestGeneral.setRequestURI("/preview/services/pipelines/general");
            
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem customResolvedSiteMapItem = vhosts.matchSiteMapItem(requestCustom);
                
                // because the /preview/services/pipelines/custom matches a sitemap item containing a custom named pipeline
                assertTrue("Expected pipeline name is 'myCustomPipeLine' ", "myCustomPipeLine".equals(customResolvedSiteMapItem.getNamedPipeline()));
                
                ResolvedSiteMapItem generalResolvedSiteMapItem = vhosts.matchSiteMapItem(requestGeneral);
                
                // because the /preview/services/pipelines/general matches a sitemap item NOT containing a custom named pipeline, this having the one
                // from the SiteMount /preview/services
                assertTrue("Expected pipeline name is 'JaxrsPipeline' ", "JaxrsPipeline".equals(generalResolvedSiteMapItem.getNamedPipeline()));
                
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        
        /*
         * On the general virtualhosts, we have set a property hst:homepage = 'home'. All virtual hosts and sitemount's which do not specify a homepage
         * theirselves must inherit this value. 
         */
        @Test 
        public void testHomePage(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setRequestURI("");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                // since the requestURI is empty, we expect a fallback to the configured homepage:
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The expected id of the resolved sitemap item is 'home'", "home".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /*
         * We now make a request uri to a sitemap item that can not be matched 
         */
        @Test
        public void testNonMatchingSiteMapItem(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setRequestURI("/preview/x/y/z/a/b/c");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertNull("resolvedSiteMapItem must be null for non matching URI", resolvedSiteMapItem);
                // there should be though a sitemount, namely 'preview'
                ResolvedSiteMount resolvedSiteMount =  vhosts.matchSiteMount(request);
                assertNotNull("The sitemount should be there still", resolvedSiteMount);
                assertTrue("The preview SiteMount must have '/preview' as pathInfoPrefix", "/preview".equals(resolvedSiteMount.getResolvedPathInfoPrefix()));
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }

       
        /*
         * We also have a test host, that has configuration like this:
         * 
         * test  (host)
         *    `onehippo (host, homepage = myhometest1)
         *        |-www  (host, homepage = myhometest2) 
         *        |   `hst:root (sitemount)
         *        |        ` services (sitemount, homepage = myhometest3)
         *        `preview
         *             `hst:root  (sitemount)
         *                  ` services (sitemount)
         *                  
         */
        @Test 
        public void testOtherHosts(){
            
            // first test
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("www.onehippo.test");
            request.setRequestURI("/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
                // second test: change request uri to "" : the expected resolvedSiteMapItem is now the homepage from the www.onehippo.test, thus myhometest2
                // since we have a parameter configured as testparam = ${1} and the expected match for /myhometest2 = _default_ , we should have myparam='myhometest2'
                request.setRequestURI("");
                resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("We expect the parameter 'testparam to resolve to 'myhometest2' ","myhometest2".equals(resolvedSiteMapItem.getParameter("testparam")));
                
                // third test: change request uri to "/preview" : the expected resolvedSiteMapItem is now the homepage from the preview.onehippo.test, thus myhometest1 from the host onehippo.org
                // since we have a parameter configured as testparam = ${1} and the expected match for /myhometest1 = _default_ , we should have myparam='myhometest1'
                request.setRequestURI("/preview");
                resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("We expect the parameter 'testparam to resolve to 'myhometest1' ","myhometest1".equals(resolvedSiteMapItem.getParameter("testparam")));
                
                // fourth test: change request uri to "/services" : the expected resolvedSiteMapItem is now the homepage from the www.onehippo.test/services, thus myhometest3
                // since we have a parameter configured as testparam = ${1} and the expected match for /myhometest3 = _default_ , we should have myparam='myhometest3'
                request.setRequestURI("/services");
                resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                assertTrue("We expect the parameter 'testparam to resolve to 'myhometest3' ","myhometest3".equals(resolvedSiteMapItem.getParameter("testparam")));
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
}
