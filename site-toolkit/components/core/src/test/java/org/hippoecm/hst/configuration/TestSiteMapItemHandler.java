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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandler;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestSiteMapItemHandler extends AbstractSpringTestCase {

        private VirtualHostsManager virtualHostsManager;
        private HstURLFactory hstURLFactory;
        protected ServletConfig servletConfig;
        protected HstContainerConfig requestContainerConfig;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            this.virtualHostsManager = getComponent(VirtualHostsManager.class.getName());
            this.hstURLFactory = getComponent(HstURLFactory.class.getName());
            this.servletConfig = (ServletConfig) getComponent(ServletConfig.class.getName());
            this.requestContainerConfig = new HstContainerConfigImpl(this.servletConfig.getServletContext(), getClass().getClassLoader());
        }

     
        @Test
        public void testNoopItemHandlerNoWildCardMatch(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/handler_nooptest");
            request.setContextPath("/site");
            
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMount mount = vhosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                request.setAttribute(ContainerConstants.RESOLVED_SITEMOUNT, mount);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_nooptest' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_nooptest".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                
                String[] handlersIds = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerIds();
                
                assertNotNull("There must be a handler on the resolvedSiteMapItem for '/handler_nooptest'",handlersIds);
                assertTrue("There must be exactly one handler and it should be browser_redirecthandler",handlersIds.length == 1 && handlersIds[0].equals("noophandler"));
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = virtualHostsManager.getSiteMapItemHandlerFactory();
                
                assertNotNull(siteMapItemHandlerFactory);
                
                HstSiteMapItemHandlerConfiguration handlerConfig = mount.getSiteMount().getHstSite().getSiteMapItemHandlersConfiguration().getSiteMapItemHandlerConfiguration(handlersIds[0]);
                
                assertNotNull("The HstSiteMapItemHandlerConfiguration must not be null for '"+handlersIds[0]+"'",handlerConfig);
                assertTrue("The siteMapItemHandlerClassName should be 'org.hippoecm.hst.test.sitemapitemhandler.NoopHandler' but was '"+handlerConfig.getSiteMapItemHandlerClassName()+"'","org.hippoecm.hst.test.sitemapitemhandler.NoopHandler".equals(handlerConfig.getSiteMapItemHandlerClassName()));
                                                                       
                try {
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                    assertNotNull("There should be created a siteMapHandler", siteMapHandler);
                    
                    // the property 'unittestproject:somestring' is ${myparam} which is configured on the sitemap item and should be resolved to /home for getProperty
                    // for getRawProperty, we should find ${myparam} as value
                    String myStringParam =  siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somestring", resolvedSiteMapItem, String.class);
                    String myStringRawParam =  siteMapHandler.getSiteMapItemHandlerConfiguration().getRawProperty("unittestproject:somestring", String.class);
                    assertTrue("myparam must be '/home' and myRawParam must be '${myparam}'", "/home".equals(myStringParam) && "${myparam}".equals(myStringRawParam));
                    
                    
                    // the property 'unittestproject:somestrings' contains in configuration val1, val2 and ${1}. The current sitemap item did not involve a wildcard, so ${1} should be set to null for getProperty.
                    // for the getRawProperty, ${1} should be there
                    String[] myStringParams = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somestrings", resolvedSiteMapItem, String[].class);
                    String[] myStringRawParams = siteMapHandler.getSiteMapItemHandlerConfiguration().getRawProperty("unittestproject:somestrings", String[].class);

                    assertTrue("We expect 3 params for unittestproject:somestrings for getProperty and 3 for getRawProperty",myStringParams.length == 3 && myStringRawParams.length == 3);
                    assertTrue(myStringParams[0].equals(myStringRawParams[0]));
                    assertTrue(myStringParams[1].equals(myStringRawParams[1]));
                    // the myStringParams[2] which was ${1} should be set to null
                    assertFalse(myStringRawParams[2].equals(myStringParams[2]));
                    
                    // test dates
                    Calendar myCal = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somedate", resolvedSiteMapItem, Calendar.class);
                    Calendar[] myCals = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somedates", resolvedSiteMapItem, Calendar[].class);
                    
                    assertNotNull(myCal);                    
                    assertTrue(myCals.length == 2);
                    
                    // test booleans
                    Boolean bool = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:someboolean", resolvedSiteMapItem, Boolean.class);
                    Boolean[] bools = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somebooleans", resolvedSiteMapItem, Boolean[].class);
                    
                    assertTrue(Boolean.TRUE.equals(bool));
                    assertTrue(bools.length == 2 && Boolean.TRUE.equals(bools[0]) && Boolean.FALSE.equals(bools[1]));
                    
                    // test longs
                    Long myLong = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somelong", resolvedSiteMapItem, Long.class);
                    Long[] myLongs = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somelongs", resolvedSiteMapItem, Long[].class);
                    
                    assertNotNull(myLong);                    
                    assertTrue(myLongs.length == 2);
                    
                    // test doubles
                    Double myDouble = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somedouble", resolvedSiteMapItem, Double.class);
                    Double[] myDoubles = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somedoubles", resolvedSiteMapItem, Double[].class);
                    
                    assertNotNull(myDouble);                    
                    assertTrue(myDoubles.length == 2);
                   
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
                
                
            }catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        
        @Test
        public void testNoopItemHandlerWithWildCardMatch(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/handler_nooptest/foo");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMount mount = vhosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                request.setAttribute(ContainerConstants.RESOLVED_SITEMOUNT, mount);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_nooptest/_default_' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_nooptest/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
                 
                String[] handlersIds = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerIds();
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = virtualHostsManager.getSiteMapItemHandlerFactory();
                HstSiteMapItemHandlerConfiguration handlerConfig = mount.getSiteMount().getHstSite().getSiteMapItemHandlersConfiguration().getSiteMapItemHandlerConfiguration(handlersIds[0]);
                                                                       
                try {
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                    // the property 'unittestproject:somestrings' contains in configuration val1, val2 and ${1}. The current sitemap item DID  involve a wildcard, so ${1} should
                    // now resolve to 'foo'
                    String[] myStringParams = siteMapHandler.getSiteMapItemHandlerConfiguration().getProperty("unittestproject:somestrings", resolvedSiteMapItem, String[].class);
                    String[] myStringRawParams = siteMapHandler.getSiteMapItemHandlerConfiguration().getRawProperty("unittestproject:somestrings", String[].class);

                    assertTrue(myStringParams[2].equals("foo"));
                    assertTrue(myStringRawParams[2].equals("${1}"));
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
                
            }catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        @Test
        public void testMultipleNoopItemHandlers(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/multiplehandler_example/foo/bar");
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMount mount = vhosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                request.setAttribute(ContainerConstants.RESOLVED_SITEMOUNT, mount);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'multiplehandler_wildcardexample/_default_/_default_' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "multiplehandler_example/_default_/_default_".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
               
                String[] handlersIds = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerIds();
                
                // assert we have two handlers:
                
                assertTrue("for '/multiplehandler_wildcardexample/foo/bar' we expect two handlers but we found '"+handlersIds.length+"'",handlersIds.length == 2);
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = virtualHostsManager.getSiteMapItemHandlerFactory();
                
                ResolvedSiteMapItem processedSiteMapItem = resolvedSiteMapItem;
                for(String handlerId : handlersIds){
                    HstSiteMapItemHandlerConfiguration handlerConfig = mount.getSiteMount().getHstSite().getSiteMapItemHandlersConfiguration().getSiteMapItemHandlerConfiguration(handlerId);
                    try {
                        HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                        processedSiteMapItem = siteMapHandler.process(processedSiteMapItem, request, response);
                    } catch (HstSiteMapItemHandlerException e){
                        fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                    }
                }
                
                // because we have configured to sitemapHandlers that do not really do something (NoopExampleHandler1 and NoopExampleHandler2), we expect the same resolved sitemap item.
                assertTrue("expectede the original resolved sitemap item back because the handlers are Noop",processedSiteMapItem == resolvedSiteMapItem);
            }catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
        /**
         * <p>
         * This is a test that ensure that the {@link BrowserRedirectHandler} returns <code>null</code> for {@link HstSiteMapItemHandler#process(ResolvedSiteMapItem, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} and
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
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/handler_browser_redirecttest");
            request.setContextPath("/site");
            
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMount mount = vhosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                request.setAttribute(ContainerConstants.RESOLVED_SITEMOUNT, mount);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                
                assertTrue("The expected id of the resolved sitemap item is 'handler_browser_redirecttest' but was '"+resolvedSiteMapItem.getHstSiteMapItem().getId()+ "'", "handler_browser_redirecttest".equals(resolvedSiteMapItem.getHstSiteMapItem().getId()));
               
                String[] handlersIds = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerIds();
                
                assertNotNull("There must be  a handler on the resolvedSiteMapItem for '/handler_browser_redirecttest'",handlersIds);
                assertTrue("There must be exactly one handler and it should be browser_redirecthandler",handlersIds.length == 1 && handlersIds[0].equals("browser_redirecthandler"));
                
                HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = virtualHostsManager.getSiteMapItemHandlerFactory();
                
                assertNotNull(siteMapItemHandlerFactory);
                
                HstSiteMapItemHandlerConfiguration handlerConfig = mount.getSiteMount().getHstSite().getSiteMapItemHandlersConfiguration().getSiteMapItemHandlerConfiguration(handlersIds[0]);
                
                assertNotNull("The HstSiteMapItemHandlerConfiguration must not be null for '"+handlersIds[0]+"'",handlerConfig);
                
                assertTrue("The siteMapItemHandlerClassName should be 'org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandler' but was '"+handlerConfig.getSiteMapItemHandlerClassName()+"'","org.hippoecm.hst.test.sitemapitemhandler.BrowserRedirectHandler".equals(handlerConfig.getSiteMapItemHandlerClassName()));
                                                                       
                try {
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                    assertNotNull("There should be created a siteMapHandler", siteMapHandler);
                    
                    HstSiteMapItemHandler siteMapHandler2 =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                    
                    assertTrue("The exact same instance should be returned the second time by the siteMapItemHandlerFactory ", siteMapHandler == siteMapHandler2);
                    
                    resolvedSiteMapItem = siteMapHandler2.process(resolvedSiteMapItem, request, response);
                    
                    assertNull("the BrowserRedirectHandler should return null ", resolvedSiteMapItem);
                    
                    // the redirect handler should have set a redirect to /home:
                    
                    String redirected = response.getRedirectedUrl();
                    
                    assertTrue("We expect the BrowserRedirectHandler to redirect to '/home' but was '"+redirected+"'", "/home".equals(redirected));
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        @Test
        public void testSiteMapItemRedirectSiteMapItemHandler(){
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.addHeader("Host", "127.0.0.1");
            request.setRequestURI("/handler_sitemapitem_redirecttest"); 
            request.setContextPath("/site");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMount mount = vhosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
                request.setAttribute(ContainerConstants.RESOLVED_SITEMOUNT, mount);
                
                HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(request, response);
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(hstContainerURL);
                String[] handlersIds = resolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerIds();
                try {
                    HstSiteMapItemHandlerFactory siteMapItemHandlerFactory = virtualHostsManager.getSiteMapItemHandlerFactory();
                    
                    assertNotNull(siteMapItemHandlerFactory);
                    
                    HstSiteMapItemHandlerConfiguration handlerConfig = mount.getSiteMount().getHstSite().getSiteMapItemHandlersConfiguration().getSiteMapItemHandlerConfiguration(handlersIds[0]);
                    HstSiteMapItemHandler siteMapHandler =  siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
                    
                    ResolvedSiteMapItem redirectedResolvedSiteMapItem = siteMapHandler.process(resolvedSiteMapItem, request, response);
                    
                    assertNotNull(redirectedResolvedSiteMapItem);
                    
                    // we expect the redirectedResolvedSiteMapItem to point to /home
                    
                    assertTrue("We should have a redirected new sitemap item and not the same one we already had.", resolvedSiteMapItem != redirectedResolvedSiteMapItem);
                    
                    assertTrue("the new redirected resolved sitemapitem should have the exact same sitemount instance ", resolvedSiteMapItem.getResolvedSiteMount() == redirectedResolvedSiteMapItem.getResolvedSiteMount());
                    
                    assertTrue("We expect the redirected resolved sitemapitem to have pathInfo 'home' but it was '"+redirectedResolvedSiteMapItem.getPathInfo()+"'", "home".equals(redirectedResolvedSiteMapItem.getPathInfo()));
                    
                } catch (HstSiteMapItemHandlerException e){
                    fail("Failed to create HstSiteMapItemHandler instance: " + e.getMessage());
                }
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        
}
