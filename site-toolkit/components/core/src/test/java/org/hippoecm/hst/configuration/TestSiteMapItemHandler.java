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
package org.hippoecm.hst.configuration;


import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.test.sitemapitemhandler.AbstractTestHstSiteItemMapHandler;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestSiteMapItemHandler extends AbstractTestConfigurations {

        private HstManager hstSitesManager;
        private HstURLFactory hstURLFactory;
        protected ServletConfig servletConfig;
        protected HstContainerConfig requestContainerConfig;

        @Override
        @Before
        public void setUp() throws Exception {
            super.setUp();
            this.hstSitesManager = getComponent(HstManager.class.getName());
            this.hstURLFactory = getComponent(HstURLFactory.class.getName());
            this.servletConfig = (ServletConfig) getComponent(ServletConfig.class.getName());
            this.requestContainerConfig = new HstContainerConfigImpl(null, getClass().getClassLoader());
        }

     
        @Test
        public void testNoopItemHandlerNoWildCardMatch(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", "localhost");
            request.setRequestURI("/site/handler_nooptest");
            request.setContextPath("/site");
            
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_nooptest' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_nooptest".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
                List<HstSiteMapItemHandlerConfiguration> handlerConfigrations = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
                
                
                assertNotNull("There must be a handler on the resolvedSiteMapItem for '/handler_nooptest'",handlerConfigrations.size() > 0);
                assertTrue("There must be exactly one handler and it should be browser_redirecthandler",handlerConfigrations.size() == 1 && handlerConfigrations.get(0).getName().equals("noophandler"));
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                
                assertNotNull(siteMapItemHandlerFactory);
               
                assertTrue("The siteMapItemHandlerClassName should be 'org.hippoecm.hst.test.sitemapitemhandler.NoopHandlerItem' but was '"+handlerConfigrations.get(0).getSiteMapItemHandlerClassName()+"'","org.hippoecm.hst.test.sitemapitemhandler.NoopHandlerItem".equals(handlerConfigrations.get(0).getSiteMapItemHandlerClassName()));
                                                                       
                try {
                    HstSiteMapItemHandler smih =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfigrations.get(0));
                    assertNotNull("There should be created a siteMapHandler", smih);

                    assertTrue(smih instanceof AbstractTestHstSiteItemMapHandler);
                    AbstractTestHstSiteItemMapHandler siteMapItemHandler = (AbstractTestHstSiteItemMapHandler)smih;
                    // the property 'unittestproject:somestring' is ${myparam} which is configured on the sitemap item and should be resolved to /home for getProperty
                    // for getRawProperty, we should find ${myparam} as value
                    String myStringParam =  siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somestring", resolvedSiteMapItem, String.class);
                    String myStringRawParam =  siteMapItemHandler.getHandlerConfig().getRawProperty("unittestproject:somestring", String.class);
                    assertTrue("myparam must be '/home' and myRawParam must be '${myparam}'", "/home".equals(myStringParam) && "${myparam}".equals(myStringRawParam));
                    
                    
                    // the property 'unittestproject:somestrings' contains in configuration val1, val2 and ${1}. The current sitemap item did not involve a wildcard, so ${1} should be set to null for getProperty.
                    // for the getRawProperty, ${1} should be there
                    String[] myStringParams = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somestrings", resolvedSiteMapItem, String[].class);
                    String[] myStringRawParams = siteMapItemHandler.getHandlerConfig().getRawProperty("unittestproject:somestrings", String[].class);

                    assertTrue("We expect 3 params for unittestproject:somestrings for getProperty and 3 for getRawProperty",myStringParams.length == 3 && myStringRawParams.length == 3);
                    assertTrue(myStringParams[0].equals(myStringRawParams[0]));
                    assertTrue(myStringParams[1].equals(myStringRawParams[1]));
                    // the myStringParams[2] which was ${1} should be set to null
                    assertFalse(myStringRawParams[2].equals(myStringParams[2]));
                    
                    // test dates
                    Calendar myCal = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somedate", resolvedSiteMapItem, Calendar.class);
                    Calendar[] myCals = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somedates", resolvedSiteMapItem, Calendar[].class);
                    
                    assertNotNull(myCal);                    
                    assertTrue(myCals.length == 2);
                    
                    // test booleans
                    Boolean bool = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:someboolean", resolvedSiteMapItem, Boolean.class);
                    Boolean[] bools = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somebooleans", resolvedSiteMapItem, Boolean[].class);
                    
                    assertTrue(Boolean.TRUE.equals(bool));
                    assertTrue(bools.length == 2 && Boolean.TRUE.equals(bools[0]) && Boolean.FALSE.equals(bools[1]));
                    
                    // test longs
                    Long myLong = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somelong", resolvedSiteMapItem, Long.class);
                    Long[] myLongs = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somelongs", resolvedSiteMapItem, Long[].class);
                    
                    assertNotNull(myLong);                    
                    assertTrue(myLongs.length == 2);
                    
                    // test doubles
                    Double myDouble = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somedouble", resolvedSiteMapItem, Double.class);
                    Double[] myDoubles = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somedoubles", resolvedSiteMapItem, Double[].class);
                    
                    assertNotNull(myDouble);                    
                    assertTrue(myDoubles.length == 2);
                   
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
                
                
            } catch (ContainerException e) {
                fail("ContainerException " + e);
            }
        }
        
        
        @Test
        public void testNoopItemHandlerWithWildCardMatch(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", "localhost");
            request.setRequestURI("/site/handler_nooptest/foo");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_nooptest/_default_' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_nooptest/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                 
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                List<HstSiteMapItemHandlerConfiguration> handlerConfigrations = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
                                                                     
                try {
                    HstSiteMapItemHandler smih =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfigrations.get(0));
                    // the property 'unittestproject:somestrings' contains in configuration val1, val2 and ${1}. The current sitemap item DID  involve a wildcard, so ${1} should
                    // now resolve to 'foo'

                    assertTrue(smih instanceof AbstractTestHstSiteItemMapHandler);
                    AbstractTestHstSiteItemMapHandler siteMapItemHandler = (AbstractTestHstSiteItemMapHandler)smih;
                    String[] myStringParams = siteMapItemHandler.getHandlerConfig().getProperty("unittestproject:somestrings", resolvedSiteMapItem, String[].class);
                    String[] myStringRawParams = siteMapItemHandler.getHandlerConfig().getRawProperty("unittestproject:somestrings", String[].class);

                    assertTrue(myStringParams[2].equals("foo"));
                    assertTrue(myStringRawParams[2].equals("${1}"));
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
                
            }catch (ContainerException e) {
                e.printStackTrace();
            }
        }
        
        @Test
        public void testMultipleNoopItemHandlers(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", "localhost");
            request.setRequestURI("/site/multiplehandler_example/foo/bar");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'multiplehandler_wildcardexample/_default_/_default_' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "multiplehandler_example/_default_/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
               
                List<HstSiteMapItemHandlerConfiguration> handlerConfigrations = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
                
                // assert we have two handlers:
                
                assertTrue("for '/multiplehandler_wildcardexample/foo/bar' we expect two handlers but we found '"+handlerConfigrations.size()+"'",handlerConfigrations.size() == 2);
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                
                ResolvedSiteMapItem processedSiteMapItem = resolvedSiteMapItem;
                for( HstSiteMapItemHandlerConfiguration handlerConfig : handlerConfigrations){
                    try {
                        HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                        processedSiteMapItem = siteMapHandler.process(processedSiteMapItem, request, response);
                    } catch (HstSiteMapItemHandlerException e){
                        fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                    }
                }
                
                // because we have configured to sitemapHandlers that do not really do something (NoopExampleHandlerItem1 and NoopExampleHandlerItem2), we expect the same resolved sitemap item.
                assertTrue("expectede the original resolved sitemap item back because the handlers are Noop",processedSiteMapItem == resolvedSiteMapItem);
            }catch (ContainerException e) {
                e.printStackTrace();
            }
        }
        
        /**
         * <p>
         * This is a test that ensure that the {@link org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandlerItem} returns <code>null</code> for {@link HstSiteMapItemHandler#process(ResolvedSiteMapItem, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} and
         * has set a redirect on the response. It terminates the request processing because the {@link ResolvedSiteMapItem} is set to <code>null</code>.
         * </p>
         * The redirect value is configured in the sitemapitemhandlers.xml as:
         * <pre>
         *  <code>
         *  &lt;sv:property sv:name="unittestproject:redirectto" sv:type="String"&gt;
         *       &lt;sv:value&gt;${redirectto}&lt;/sv:value&gt;
         *  &lt;/sv:property&gt;
         *  </code>
         * </pre>
         * where ${redirectto} is fetched from the matched sitemap item which looks like:
         * 
         * <pre>
         *  <code>
         *   &lt;sv:property sv:name="hst:parameternames" sv:type="String"&gt;
         *       &lt;sv:value&gt;redirectto&lt;/sv:value&gt;
         *   &lt;/sv:property&gt;
         *   &lt;sv:property sv:name="hst:parametervalues" sv:type="String"&gt;
         *       &lt;sv:value&gt;/home&lt;/sv:value&gt;
         *   &lt;/sv:property&gt;
         *  </code>
         * </pre>
         */
        @Test
        public void testBrowserRedirectSiteMapItemHandler(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", "localhost");
            request.setRequestURI("/site/handler_browser_redirecttest");
            request.setContextPath("/site");
            
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_browser_redirecttest' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_browser_redirecttest".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
               
                List<HstSiteMapItemHandlerConfiguration> handlerConfigrations = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
                
                assertTrue("There must be exactly one handler and it should be browser_redirecthandler",handlerConfigrations.size() == 1 && handlerConfigrations.get(0).getName().equals("browser_redirecthandler"));
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                
                assertNotNull(siteMapItemHandlerFactory);
                
                assertTrue("The siteMapItemHandlerClassName should be 'org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandlerItem' but was '"+handlerConfigrations.get(0).getSiteMapItemHandlerClassName()+"'","org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandlerItem".equals(handlerConfigrations.get(0).getSiteMapItemHandlerClassName()));
                                                                       
                try {
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfigrations.get(0));
                    assertNotNull("There should be created a siteMapHandler", siteMapHandler);
                    
                    HstSiteMapItemHandler siteMapHandler2 =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfigrations.get(0));
                    
                    assertTrue("The exact same instance should be returned the second time by the siteMapItemHandlerFactory ", siteMapHandler == siteMapHandler2);
                    
                    resolvedSiteMapItem = siteMapHandler2.process(resolvedSiteMapItem, request, response);
                    
                    assertNull("the BrowserRedirectHandlerItem should return null ", resolvedSiteMapItem);
                    
                    // the redirect handler should have set a redirect to /home:
                    
                    String redirected = response.getRedirectedUrl();
                    
                    assertTrue("We expect the BrowserRedirectHandlerItem to redirect to '/home' but was '"+redirected+"'", "/home".equals(redirected));
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
            } catch (ContainerException e) {
                e.printStackTrace();
            }
            
        }
        
        @Test
        public void testSiteMapItemRedirectSiteMapItemHandler(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("localhost");
            request.addHeader("Host", "localhost");
            request.setRequestURI("/site/handler_sitemapitem_redirecttest");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
                ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                List<HstSiteMapItemHandlerConfiguration> handlerConfigrations = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
                
                try {
                    HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                    
                    assertNotNull(siteMapItemHandlerFactory);
                    
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfigrations.get(0));
                    
                    ResolvedSiteMapItem redirectedResolvedSiteMapItem = siteMapHandler.process(resolvedSiteMapItem, request, response);
                    
                    assertNotNull(redirectedResolvedSiteMapItem);
                    
                    // we expect the redirectedResolvedSiteMapItem to point to /home
                    
                    assertTrue("We should have a redirected new sitemap item and not the same one we already had.", resolvedSiteMapItem != redirectedResolvedSiteMapItem);
                    
                    assertTrue("the new redirected resolved sitemapitem should have the exact same mount instance ", resolvedSiteMapItem.getResolvedMount() == redirectedResolvedSiteMapItem.getResolvedMount());
                    
                    assertTrue("We expect the redirected resolved sitemapitem to have pathInfo 'home' but it was '"+redirectedResolvedSiteMapItem.getPathInfo()+"'", "home".equals(redirectedResolvedSiteMapItem.getPathInfo()));
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
            } catch (ContainerException e) {
                e.printStackTrace();
            }
            
        }
        
        
}
