/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PageCachingValveIT extends AbstractPipelineTestCase {

    protected Session session;
    protected Boolean cacheableFlagAtSetup = null;
    protected String componentClassNameAtSetup = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCachingFlagAndComponentClass();
        
    }

    @After
    public void tearDown() throws Exception {
        resetCachingFlagAndComponentClass();
        if (session != null && session.isLive()) {
            session.logout();
        }
        super.tearDown();
    }

   
    @Test
    public void testCachedPagesAndHeaders() throws ContainerException, UnsupportedEncodingException {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[1] ;
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        MockHttpServletResponse response = executeRequest("/news", headers);

        assertTrue("HeaderComponentWithResponseHeaders should had set 'timestamp' header equal timestamp attribute",
        timestamp.equals(response.getHeader("timestamp")));

        String newTimestamp = String.valueOf(System.nanoTime());
        // replace with new timestamp. Since /news is cached, we do not expect the new timestamp on the response
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        response = executeRequest("/news", headers);

        assertTrue("Because page is from cache, timestamp should be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));

        response = executeRequest("/news/2009", headers);
        assertTrue("Because page is not from cache because different pathInfo, the timestamp should be equal to newTimestamp",
                    newTimestamp.equals(response.getHeader("timestamp")));

    }

    @Test
    public void testCachedPage_with_TTL_equal_to_expires() throws Exception {

        if (new Random().nextDouble() < 0.95) {
            // below test takes 3000 seconds at least to trigger a TTL expiration. Running this test occasionally is
            // good enough
            return;
        }
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[2] ;
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        // cache expected  3000 millisec since expires is  3000 millisec
        headers[1] = new DefaultKeyValue<>("Expires", System.currentTimeMillis() + 3000L);
        MockHttpServletResponse response = executeRequest("/news", headers);

        assertTrue("HeaderComponentWithResponseHeaders should had set 'timestamp' header equal timestamp attribute",
                timestamp.equals(response.getHeader("timestamp")));

        String newTimestamp = String.valueOf(System.nanoTime());
        // replace with new timestamp. Since /news is cached, we do not expect the new timestamp on the response
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        response = executeRequest("/news", headers);

        assertTrue("Because page is from cache, timestamp should be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));

        // wait 3000 ms: Now, TTL of the cached page should have expired and thus now 'newTimestamp' is expected
        Thread.sleep( 3000L);
        response = executeRequest("/news", headers);
        assertFalse("Page should not have been cached any more because TTL expired", timestamp.equals(response.getHeader("timestamp")));
        assertTrue("Page should not have been cached any more because TTL expired and thus new timestamp expected", newTimestamp.equals(response.getHeader("timestamp")));
    }

    @Test
    public void testPragmaNoCacheIsNotCached() throws ContainerException, UnsupportedEncodingException {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[2];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        headers[1] = new DefaultKeyValue<>("Pragma","no-cache");

        executeRequest("/news", headers);

        String newTimestamp = String.valueOf(System.nanoTime());
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        MockHttpServletResponse response = executeRequest("/news", headers);
        assertFalse("Page should not have been cached and thus a different timestamp because of Pragma no-cache",
                timestamp.equals(response.getHeader("timestamp")));
    }

    @Test
    public void testCacheControlNoCacheIsNotCached() throws ContainerException, UnsupportedEncodingException {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[2];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        headers[1] = new DefaultKeyValue<>("Cache-Control","no-cache");

        executeRequest("/news", headers);

        String newTimestamp = String.valueOf(System.nanoTime());
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        MockHttpServletResponse response = executeRequest("/news", headers);
        assertFalse("Page should not have been cached and thus a different timestamp because of Cache-Control no-cache",
                timestamp.equals(response.getHeader("timestamp")));
    }

    @Test
    public void testExpiresIs0IsNotCached() throws Exception {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[2];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        headers[1] = new DefaultKeyValue<>("Expires", 0L);

        executeRequest("/news", headers);

        String newTimestamp = String.valueOf(System.nanoTime());
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        MockHttpServletResponse response = executeRequest("/news", headers);
        assertFalse("Page should not have been cached and thus a different timestamp because of Expires 0",
                timestamp.equals(response.getHeader("timestamp")));
    }

    @Test
    public void testExpiresIsLessThan0IsNotCached() throws Exception {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[2];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        headers[1] = new DefaultKeyValue<>("Expires", -1L);

        executeRequest("/news", headers);

        String newTimestamp = String.valueOf(System.nanoTime());
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        MockHttpServletResponse response = executeRequest("/news", headers);
        assertFalse("Page should not have been cached and thus a different timestamp because of Expires -1",
                timestamp.equals(response.getHeader("timestamp")));
    }

    @Test
    public void testNoCacheHeadersBeforePageCacheValveResultDoNotInfluenceCaching() throws Exception {
        String timestamp = String.valueOf(System.nanoTime());
        KeyValue[] headers = new KeyValue[1];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);

        MockHttpServletRequest request = createServletRequest("/news");
        request.setAttribute("headers", headers);

        MockHttpServletResponse response = createServletResponse();
        // set no-cache headers BEFORE caching valve should not influence caching!
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", -1L);

        // now execute request where caching headers already have been set
        executeRequest(request, response);

        assertTrue("HeaderComponentWithResponseHeaders should had set 'timestamp' header equal timestamp attribute",
                timestamp.equals(response.getHeader("timestamp")));

        String newTimestamp = String.valueOf(System.nanoTime());
        // replace with new timestamp. Since /news is cached, we do not expect the new timestamp on the response
        // EVEN though no-cache headers were used in previous request : This is because the cache headers were set
        // BEFORE the page caching valve
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        response = executeRequest("/news", headers);

        assertTrue("Because page is from cache, timestamp should be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));

    }


    @Test
    public void testPagesWhereCookiesAreSetAfterPageCacheValveAreNotCached() throws Exception {
        String timestamp = String.valueOf(System.nanoTime());

        KeyValue[] headers = new KeyValue[1] ;
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
        KeyValue[] cookies = new KeyValue[1];
        cookies[0] = new DefaultKeyValue<>("foo","bar");

        MockHttpServletResponse response = executeRequest("/news", headers, cookies);

        assertTrue("HeaderComponentWithResponseHeaders should had set 'timestamp' header equal timestamp attribute",
                timestamp.equals(response.getHeader("timestamp")));
        assertTrue(response.getCookie("foo").getValue().equals("bar"));

        String newTimestamp = String.valueOf(System.nanoTime());
        // replace with new timestamp. Since /news is cached, we do not expect the new timestamp on the response
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        response = executeRequest("/news", headers, cookies);

        assertFalse("Because page is not from cache due to cookies, timestamp should NOT be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));
        assertTrue(newTimestamp.equals(response.getHeader("timestamp")));
        assertTrue(response.getCookie("foo").getValue().equals("bar"));

        cookies[0] = new DefaultKeyValue<>("foo","lux");
        response = executeRequest("/news", headers, cookies);
        assertTrue(response.getCookie("foo").getValue().equals("lux"));
    }

    @Test
    public void testPagesWhereCookiesAreSetBeforePageCacheValveDoNotInfluenceCaching() throws Exception {
        String timestamp = String.valueOf(System.nanoTime());
        KeyValue[] headers = new KeyValue[1];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);

        MockHttpServletRequest request = createServletRequest("/news");
        request.setAttribute("headers", headers);

        MockHttpServletResponse response = createServletResponse();
        response.addCookie(new Cookie("foo", "bar"));

        // now execute request where cookies already have been set
        executeRequest(request, response);

        assertTrue("HeaderComponentWithResponseHeaders should had set 'timestamp' header equal timestamp attribute",
                timestamp.equals(response.getHeader("timestamp")));
        assertTrue(response.getCookie("foo").getValue().equals("bar"));

        String newTimestamp = String.valueOf(System.nanoTime());
        // replace with new timestamp. Since /news is cached, we do not expect the new timestamp on the response
        // EVEN though cookies were used in previous request : This is because the cookies were set
        // BEFORE the page caching valve
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        response = executeRequest("/news", headers);

        assertTrue("Because page is from cache, timestamp should be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));

        assertNull(response.getCookie("foo"));
    }


    /**
     * due to blocking cache we have to make sure no lock on some cache segment is not freed. This
     * test assures that it works regardless runtime exceptions in hst components
     */
    @Test
    public void testPagesWithComponentRunTimeExceptionAreCached() throws ContainerException {
        String timestamp = String.valueOf(System.nanoTime());
        KeyValue[] headers = new KeyValue[1];
        headers[0] = new DefaultKeyValue<>("timestamp",timestamp);

        MockHttpServletRequest request = createServletRequest("/news");
        request.setAttribute("headers", headers);
        request.setAttribute("throwexception", "true");

        MockHttpServletResponse response = createServletResponse();
        executeRequest(request, response);

        // now do the request without the exception

        String newTimestamp = String.valueOf(System.nanoTime());
        headers[0] = new DefaultKeyValue<>("timestamp",newTimestamp);
        request = createServletRequest("/news");
        request.setAttribute("headers", headers);
        response = createServletResponse();
        executeRequest(request, response);
        assertTrue("Because page is from cache, timestamp should be equal to the earlier rendered page",
                timestamp.equals(response.getHeader("timestamp")));
    }

    /**
     * We now create x requests and start executing them with 50 threads: Note that EVEN that at 
     * start we do not have any cached page, only ONE SINGLE request will actually build the response
     * regardless with the initial concurrency : This is because we use a Blocking Cache which guards 
     * against stampeding herds as well
     */
    @Test
    public void testPageCacheConcurrency() throws Exception {

        final ExecutorService executorService = Executors.newFixedThreadPool(50);
        // only run 1/50 of the time a large test
        int nrJobs = getNrJobs(200, 100000, 0.98);

        // even though every MockedRequestExecutor the gets its #call invoked
        // will have a different timestamp, we expect due to caching, that all
        // returned results still contain the same result as the one we already did above
        Collection<MockedRequestExecutor> jobs = new ArrayList<MockedRequestExecutor>(nrJobs);
        for (int i = 0; i < nrJobs; i++) {
            jobs.add(new MockedRequestExecutor());
        }
        
        final List<Future<MockHttpServletResponse>> futures = executorService.invokeAll(jobs);        
        executorService.shutdown(); // Disable new tasks from being submitted
       
        // Wait a while for existing tasks to terminate
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            executorService.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                fail("Pool did not terminate");
            }
        }

        MockHttpServletResponse current = null;
        for (Future<MockHttpServletResponse> future : futures) {
            if (!future.isDone()) {
                fail("unfinished requests");
            }
            MockHttpServletResponse next = future.get();
            if (current == null) {
                current = next;
                continue;
            }
            assertTrue(current.getHeader("timestamp").equals(next.getHeader("timestamp")));
            current = next;
        }
        
    }

    /**
     * return a low value if the random double is lower than 'odds', else returns the high value
     */
    private int getNrJobs(final int low, final int high, final double odds) {
        // only of random is > odds, return the high
        if (new Random().nextDouble() > odds) {
            return high;
        } else {
            return low;
        }
    }

    @Test
    public void testPageConcurrencyDuringCacheClearing() {
        // the boolean for boolean allResponsesAreExactlyTheSameCachedInstance should become false
        boolean allResponsesAreExactlyTheSameCachedInstance = true;
        try {

            // only 1/50 of the time run a large test
            int nrJobs = getNrJobs(200, 100000, 0.98);

            final ExecutorService executorService = Executors.newFixedThreadPool(50);
            Collection<MockedRequestExecutor> jobs = new ArrayList<MockedRequestExecutor>(nrJobs);
            for (int i = 0; i < nrJobs; i++) {
                if (i % 100 == 0) {
                 jobs.add(new MockedRequestExecutor(true));  
                } else {
                 jobs.add(new MockedRequestExecutor());
                }
            }
            final List<Future<MockHttpServletResponse>> futures = executorService.invokeAll(jobs);
            
            
            executorService.shutdown(); 
            
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                    fail("Pool did not terminate");
                }
            }
    
            MockHttpServletResponse current = null;

            for (Future<MockHttpServletResponse> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished requests");
                }
                MockHttpServletResponse next = future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                if(!current.getHeader("timestamp").equals(next.getHeader("timestamp"))) {
                    allResponsesAreExactlyTheSameCachedInstance = false;
                }
                current = next;
            }
        } catch (Throwable e) {
            fail(e.toString()); 
        }
        assertFalse("Not all page responses should be the same due to cache clearing",allResponsesAreExactlyTheSameCachedInstance);
    }

    class MockedRequestExecutor implements Callable<MockHttpServletResponse> {

        boolean firstClearCache = false;
        
        public MockedRequestExecutor() {
            
        }
        
        public MockedRequestExecutor(final boolean clear) {
            firstClearCache = clear;
        }

        @Override
        public MockHttpServletResponse call() throws Exception {
            if (firstClearCache) {
                HstCache cache = HstServices.getComponentManager().getComponent("pageCache");
                cache.clear();
            }
            String timestamp = String.valueOf(System.nanoTime());
            KeyValue[] headers = new KeyValue[1];
            headers[0] = new DefaultKeyValue<>("timestamp",timestamp);
            return executeRequest("/news", headers);
        }
    }

    public static class HeaderComponentWithResponseHeaders extends GenericHstComponent {
        @Override
        public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
            KeyValue[] headers =  (KeyValue[])request.getAttribute("headers");
            if (headers != null) {
                for (KeyValue header : headers) {
                    if (header.getKey().equals("Expires")) {
                        response.setDateHeader(String.valueOf(header.getKey()), new Long(String.valueOf(header.getValue())));
                    } else {
                        response.setHeader(String.valueOf(header.getKey()), String.valueOf(header.getValue()));
                    }
                }
            }

            KeyValue[] cookies =  (KeyValue[])request.getAttribute("cookies");
            if (cookies != null) {
                for (KeyValue cookie : cookies) {
                    response.addCookie(new Cookie(String.valueOf(cookie.getKey()), String.valueOf(cookie.getValue())));
                }
            }

            if (request.getAttribute("throwexception") != null) {
                throw new RuntimeException("Forced runtime exception");
            }
        }
    }

    protected  MockHttpServletResponse executeRequest(final String pathInfo, final KeyValue[] headers, final KeyValue[] cookies) throws ContainerException {
        MockHttpServletRequest servletRequest = createServletRequest(pathInfo);
        servletRequest.setAttribute("headers", headers);
        servletRequest.setAttribute("cookies", cookies);
        MockHttpServletResponse servletResponse = createServletResponse();
        executeRequest(servletRequest,  servletResponse);
        return servletResponse;
    }

    protected  MockHttpServletResponse executeRequest(final String pathInfo, final KeyValue[] headers) throws ContainerException {
        MockHttpServletRequest servletRequest = createServletRequest(pathInfo);
        servletRequest.setAttribute("headers", headers);
        MockHttpServletResponse servletResponse = createServletResponse();
        executeRequest(servletRequest,  servletResponse);
        return servletResponse;
    }

    protected void executeRequest(final MockHttpServletRequest servletRequest, final MockHttpServletResponse servletResponse) throws ContainerException {
        HstRequestContext requestContext = resolveRequest(servletRequest, servletResponse);
        try {
            this.defaultPipeline.invoke(this.requestContainerConfig, requestContext, servletRequest, servletResponse);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.cleanup(this.requestContainerConfig, requestContext, servletRequest, servletResponse);
        }
    }


    protected Session getSession(){
        try {
            if(this.session != null && this.session.isLive()) {
                return this.session;
            } else {
                Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName()+".delegating");
                this.session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            }
            return this.session;
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected MockHttpServletRequest createServletRequest(String pathInfo) {
        ServletContext servletContext = HstServices.getComponentManager().getComponent(ServletContext.class.getName());
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setContextPath("/site");
        request.setPathInfo(pathInfo);
        request.addHeader("Host", request.getServerName());

        request.setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        return request;
    }

    protected MockHttpServletResponse createServletResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }

    protected void setCachingFlagAndComponentClass() throws Exception {

        Node hstHosts = getSession().getNode("/hst:hst/hst:hosts");
        if (hstHosts.hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            cacheableFlagAtSetup = hstHosts.getProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE).getBoolean();
        }
        // now set cacheable flag to true
        hstHosts.setProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE, true);

        Node headerComponent  = getSession().getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
        if (headerComponent.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME)) {
            componentClassNameAtSetup = headerComponent.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString();
        }

        headerComponent.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, HeaderComponentWithResponseHeaders.class.getName());
        getSession().save();
        Thread.sleep(100);
    }


    protected void resetCachingFlagAndComponentClass() throws Exception {
        Node hstHosts = getSession().getNode("/hst:hst/hst:hosts");
        if (cacheableFlagAtSetup == null) {
            // remove the property
            if (hstHosts.hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
                hstHosts.getProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE).remove();
            }
        } else {
            // set to original value
            if (hstHosts.hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
                hstHosts.setProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE, cacheableFlagAtSetup.booleanValue());
            }
        }

        Node headerComponent  = getSession().getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
        if (componentClassNameAtSetup == null) {
            // remove the property
            if (headerComponent.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME)) {
                headerComponent.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).remove();
            }
        } else {
            // set to original value
            if (headerComponent.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME)) {
                headerComponent.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, componentClassNameAtSetup);
            }
        }

        getSession().save();

    }

}
