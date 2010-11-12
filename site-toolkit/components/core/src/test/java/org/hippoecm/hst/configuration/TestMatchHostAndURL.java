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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestMatchHostAndURL extends AbstractSpringTestCase {

        private HstManager hstSitesManager;
        private HstURLFactory hstURLFactory;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            this.hstSitesManager = getComponent(HstManager.class.getName());
            this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        }

     
        @Test
        public void testDefaultHost(){
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                assertTrue("Expected from the hst testcontents default hostname to be 127.0.0.1. ", "127.0.0.1".equals(vhosts.getDefaultHostName()));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
            
        }
        
    
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing Mount should be live
         */
        @Test
        public void testMatchNoContextPath(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/news/2009");
            request.setContextPath("");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getRelativeContentPath()+ "'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the requestURI did not match the preview Mount, so our Mount must be live:
                assertFalse("We should have a match in LIVE ",resolvedSiteMapItem.getResolvedMount().getMount().isPreview());
                assertTrue("The live Mount must have an empty string \"\" as mountPath", "".equals(resolvedSiteMapItem.getResolvedMount().getResolvedMountPath()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing Mount should be preview
         */
        @Test
        public void testMatchPreviewNoContextPath(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/preview/news/2009");
            request.setContextPath("");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the Mount from the requestURI should match the preview Mount, so our Mount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedMount().getMount().isPreview());
                assertTrue("The preview Mount must have '/preview' as mountPath", "/preview".equals(resolvedSiteMapItem.getResolvedMount().getResolvedMountPath()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * Default, when there is no hst:versioninpreviewheader configured, or, since we configure hst:versioninpreviewheader = true on the 
         * base hstvirtualhosts configuration, all Mount's by default should return true: 
         */
        @Test
        public void testVersionInPreviewHeaderDefaultTrue(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/preview/news/2009");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
               
                assertTrue("We expect the mount to return true for version in preview header", mount.getMount().isVersionInPreviewHeader());
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest *does* have a context path
         * The backing Mount should be live
         */
        @Test
        public void testMatchWithContextPath(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setContextPath("/site");
            request.setRequestURI("/site/news/2009");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                assertFalse("We should have a match in LIVE ",resolvedSiteMapItem.getResolvedMount().getMount().isPreview());
                assertTrue("The live Mount must have an empty string \"\" as mountPath", "".equals(resolvedSiteMapItem.getResolvedMount().getResolvedMountPath()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing Mount should be preview
         */
        @Test
        public void testMatchPreviewWithContextPath(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setContextPath("/site");
            request.setRequestURI("/site/preview/news/2009");
            try {
                
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the Mount from the requestURI should match the preview Mount, so our Mount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedMount().getMount().isPreview());
                assertTrue("The preview Mount must have '/preview' as mountPath", "/preview".equals(resolvedSiteMapItem.getResolvedMount().getResolvedMountPath()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * This test should match the sitemap item /news/* which has a relative content path /News/${1}
         * The HttpServletRequest does not have a context path
         * The backing Mount should be 'preview/custompipeline'
         * This Mount is configured with a namedPipeline = 'CustomPipeline', hence, the resolvedSiteMapItem.getNamedPipeline should return 'CustomPipeline'
         * The Mount 'preview/custompipeline' should inherit the mountPoint from its parent, which is 'preview', so the same HstSite config is there, with same sitemap tree
         */
        @Test
        public void testMatchPreviewServices(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/site/preview/custompipeline/news/2009");
            request.setContextPath("/site");
            try {
                
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                // the Mount from the requestURI should match the preview Mount, so our Mount must be preview:
                assertTrue( "We should have a match in PREVIEW  ", resolvedSiteMapItem.getResolvedMount().getMount().isPreview());
                assertTrue("The preview Mount must have '/preview/custompipeline' as mountPath", "/preview/custompipeline".equals(resolvedSiteMapItem.getResolvedMount().getResolvedMountPath()));
                
                // because the /preview/services Mount has configured a different pipeline, the resolvedSiteMapItem should reflect this:
                assertTrue("Expected pipeline name is 'CustomPipeline' ", "CustomPipeline".equals(resolvedSiteMapItem.getNamedPipeline()));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * We now make a request uri to 
         * /preview/custompipeline/pipelines/custom   and
         * /preview/custompipeline/pipelines/general  where the custom has a specific pipeline configured on the matching sitemap item and
         * general should fallback to the pipeline configured on the Mount '/preview/custompipeline'
         */
        @Test
        public void testSiteMapItemNamedPipeline(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest requestCustom = new MockHttpServletRequest();
            requestCustom.setLocalPort(8081);
            requestCustom.setScheme("http");
            requestCustom.setServerName("127.0.0.1");
            requestCustom.addHeader("Host", "127.0.0.1");
            requestCustom.setRequestURI("/site/preview/custompipeline/pipelines/custom");
            requestCustom.setContextPath("/site");

            MockHttpServletRequest requestGeneral = new MockHttpServletRequest();
            
            requestGeneral.setLocalPort(8081);
            requestGeneral.setScheme("http");
            requestGeneral.setServerName("127.0.0.1");
            requestGeneral.addHeader("Host", "127.0.0.1");
            requestGeneral.setRequestURI("/site/preview/custompipeline/pipelines/general");
            requestGeneral.setContextPath("/site");
            
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                
                // requestCustom
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(requestCustom), requestCustom.getContextPath(), HstRequestUtils.getRequestPath(requestCustom));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(requestCustom, response, mount);
                ResolvedSiteMapItem customResolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                // because the /preview/services/pipelines/custom matches a sitemap item containing a custom named pipeline
                assertTrue("Expected pipeline name is 'MyCustomPipeline' ", "MyCustomPipeline".equals(customResolvedSiteMapItem.getNamedPipeline()));
                
                // requestGeneral
                ResolvedMount mountGeneral = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(requestGeneral), requestGeneral.getContextPath(), HstRequestUtils.getRequestPath(requestGeneral));
                HstContainerURL hstContainerURL2 = hstURLFactory.getContainerURLProvider().parseURL(requestGeneral, response, mountGeneral);
                ResolvedSiteMapItem generalResolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL2);
                
                // because the /preview/services/pipelines/general matches a sitemap item NOT containing a custom named pipeline, this having the one
                // from the Mount /preview/services
                assertTrue("Expected pipeline name is 'CustomPipeline' but was '"+generalResolvedSiteMapItem.getNamedPipeline()+"'", "CustomPipeline".equals(generalResolvedSiteMapItem.getNamedPipeline()));
                
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        
        /*
         * On the general virtualhosts, we have set a property hst:homepage = 'home'. All virtual hosts and Mount's which do not specify a homepage
         * theirselves must inherit this value. 
         */
        @Test 
        public void testHomePage(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/site");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                // since the requestURI is empty, we expect a fallback to the configured homepage:
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
               
                assertTrue("The expected id of the resolved sitemap item is 'home'", "home".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * We now make a request uri to a sitemap item that can not be matched 
         */
        @Test
        public void testNonMatchingSiteMapItem(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/site/preview/x/y/z/a/b/c");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
               
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                // this should be the page not found sitemap item
                assertTrue("The expected id of the resolved sitemap item is 'pagenotfound'", "pagenotfound".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
            }catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * Tests for the not mounted Mounts (issitemount = false), which for example can be used in case of rest interface, which have their own custom
         * namedPipeline, and only use the mountpoint as a location for content, not for a HstSite
         */
        @Test 
        public void testMountThatIsNotMounted(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/site/preview/services");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                
                // since the requestURI is empty, we expect a fallback to the configured homepage:
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));

                assertTrue("The mount for /preview/services should return that it is not mounted ", !mount.getMount().isMapped());
                assertNull("An not mounted Mount should have a HstSite that is null", mount.getMount().getHstSite());
                assertTrue("The mountpath for /preview/services mount must be '/preview/services' but was '"+mount.getMount().getMountPath()+"'", "/preview/services".equals(mount.getMount().getMountPath()));
                assertTrue("The mountpoint for /preview/services mount must be '/hst:hst/hst:unittestsites/unittestproject-preview' but was '"+mount.getMount().getMountPoint()+"'", "/hst:hst/hst:unittestsites/unittestproject-preview".equals(mount.getMount().getMountPoint()));
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        
        /*
         * We now test with port 7979: this one is not explicitly defined in the unittest config: this means, it should default back to the matching host for port 0
         */
        @Test 
        public void testSiteWithNoConfiguredPort(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setServerPort(8180);
            // the port is part of the Host header
            request.addHeader("Host", "127.0.0.1:7979");
            request.setRequestURI("/site/home");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                
                // since the requestURI is empty, we expect a fallback to the configured homepage:
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                
                assertFalse("For port 7979 we do not have a configured a portmount, and thus we should get a mount that is live ", mount.getMount().isPreview());
                
                assertTrue("Resolved virtualhost must have the portnumber! ",mount.getResolvedVirtualHost().getPortNumber() == 7979);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The id for the resolved sitemap item must be 'home' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "home".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * We now test with port 8081: this one *is explicitly* defined in the unittest config: this means, it should be used.
         * 
         * For port 8081 we configured that it is a preview. We should thus get a Mount that has isPreview = true
         */
        @Test 
        public void testSiteWithConfiguredPort(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setServerPort(8180);
            // the port is part of the Host header
            request.addHeader("Host", "127.0.0.1:8081");
            request.setRequestURI("/site/home");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                
                // since the requestURI is empty, we expect a fallback to the configured homepage:
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                
                assertTrue("For port 8081 we do not have a configured a portmount, and thus we should get a mount that is preview ", mount.getMount().isPreview());
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The id for the resolved sitemap item must be 'home' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "home".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * We now test a match on a Mount at 127.0.0.1/hst:root/examplecontextpathonly  that has configured: 
         * 
         * hst:onlyforcontextpath = /mycontextpath
         *  
         * This means, that for a request uri like '127.0.0.1/hst:root/examplecontextpathonly/home' that in case:
         * 
         * 1) The contextPath is '/mycontextpath' it should match this Mount
         * 2) the contextPath is different than  '/mycontextpath': it should default back to the hst:root Mount
         * 
         * 
         */
        @Test 
        public void testMountOnlyForContextPath(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setServerPort(8180);
            // the port is part of the Host header
            request.addHeader("Host", "127.0.0.1");
            
            // hst:onlyforcontextpath = /mycontextpath so we start with correct context path
            request.setRequestURI("/mycontextpath/examplecontextpathonly/home");
            request.setContextPath("/mycontextpath");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                
                assertTrue("As the contextPath '/mycontextpath' matches the configured one of Mount 'examplecontextpathonly', we expect the Mount to have the name 'examplecontextpathonly'",mount.getMount().getName().equals("examplecontextpathonly"));
                
                // now change the contextPath to be unequal to the 'hst:onlyforcontextpath = /mycontextpath'. The Mount should be the hst:root Mount now
                request.setRequestURI("/mywrongpath/examplecontextpathonly/home");
                request.setContextPath("/mywrongpath");
                
                mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
               
                assertTrue("As the contextPath '/mywrongpath' does not match the configured one of Mount 'examplecontextpathonly', we expect a fallback to the Mount hst:root ",mount.getMount().getName().equals("hst:root"));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
        /*
         * We also have a test host, that has configuration like this:
         * 
         * test  (host)
         *    `onehippo (host, homepage = myhometest1)
         *        |-www  (host, homepage = myhometest2) 
         *        |   `hst:root (mount)
         *        |        |- custompipeline (mount, homepage = myhometest3)
         *        |        `- services (mount, isSiteMount = false)
         *        `preview (homepage = myhometestpreview)
         *        |    `hst:root  (mount)
         *        |        |- custompipeline (mount, homepage = mycustomhometestpreview, hst:versioninpreviewheader = false)
         *        |        `- services (mount, isSiteMount = false)
         *                  
         */
        @Test 
        public void testOtherHosts(){
            
            // first test
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("www.onehippo.test");
            request.addHeader("Host", "www.onehippo.test");
            request.setRequestURI("/site/news/2009");
            request.setContextPath("/site");
            
            MockHttpServletRequest previewRequest = new MockHttpServletRequest();
            previewRequest.setLocalPort(8081);
            previewRequest.setScheme("http");
            previewRequest.setServerName("www.onehippo.test");
            previewRequest.addHeader("Host", "preview.onehippo.test");
            previewRequest.setRequestURI("/site");
            previewRequest.setContextPath("/site");
            try {
                
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
               
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The relative content path must be '/News/2009'", "News/2009".equals(resolvedSiteMapItem.getRelativeContentPath()));
                assertTrue("The expected id of the resolved sitemap item is 'news/_default_'", "news/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
                // second test: change request uri to "" : the expected resolvedSiteMapItem is now the homepage from the www.onehippo.test, thus myhometest2
                // since we have a parameter configured as testparam = ${1} and the expected match for /myhometest2 = _default_ , we should have myparam='myhometest2'
                request.setRequestURI("/site");
                
                mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);

                
                assertTrue("We expect the parameter 'testparam to resolve to 'myhometest2' ","myhometest2".equals(resolvedSiteMapItem.getParameter("testparam")));
                
                // third test: change request hostname to "preview.onehippo.test" : the expected resolvedSiteMapItem is now the homepage from the preview.onehippo.test, thus myhometest1 from the host preview.onehippo.test
                // since we have a parameter configured as testparam = ${1} and the expected match for /myhometest1 = _default_ , we should have myparam='myhometest1'
               
                
                mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(previewRequest), previewRequest.getContextPath(), HstRequestUtils.getRequestPath(previewRequest));
                hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(previewRequest, response, mount);
                resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("We expect the parameter 'testparam to resolve to 'myhometestpreview' but it was '"+resolvedSiteMapItem.getParameter("testparam")+"'","myhometestpreview".equals(resolvedSiteMapItem.getParameter("testparam")));
                
                // fourth test: change request uri to "/custompipeline" : the expected resolvedSiteMapItem is now the homepage from the preview.onehippo.test/custompipeline, thus mycustomhometestpreview
                // since we have a parameter configured as testparam = ${1} and the expected match for /mycustomhometestpreview = _default_ , we should have myparam='mycustomhometestpreview'
                
                previewRequest.setRequestURI("/site/custompipeline");
                
                mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(previewRequest), previewRequest.getContextPath(), HstRequestUtils.getRequestPath(previewRequest));
                // The custompipeline Mount also has hst:versioninpreviewheader = false
                assertFalse("The mount for custompipeline should return false for version in preview header but returned true",mount.getMount().isVersionInPreviewHeader());
                
                hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(previewRequest, response, mount);
                resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("We expect the parameter 'testparam to resolve to 'mycustomhometestpreview' ","mycustomhometestpreview".equals(resolvedSiteMapItem.getParameter("testparam")));
                
            } catch (RepositoryNotAvailableException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        }
        
}
