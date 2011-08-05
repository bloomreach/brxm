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
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.EnumerationUtils;
import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * TestHstRequestProcessor
 * 
 * @version $Id$
 */
public class TestHstRequestProcessor {
    
    private Logger log = LoggerFactory.getLogger(TestHstRequestProcessor.class);
    
    private HstRequestProcessor requestProcessor;
    private HstContainerConfig requestContainerConfig;
    
    @Before
    public void setUp() throws Exception {
        Map<String, Pipeline> pipelinesMap = new HashMap<String, Pipeline>();
        
        HstSitePipeline basicPipeline = new HstSitePipeline();
        basicPipeline.setPreInvokingValves(new Valve [] { new SimpleValve("pre-1"), new SimpleValve("pre-2"), new SimpleValve("pre-3") });
        basicPipeline.setInvokingValves(new Valve [] { new SimpleValve("main-1"), new SimpleValve("main-2"), new SimpleValve("main-3") });
        basicPipeline.setPostInvokingValves(new Valve [] { new SimpleValve("post-1"), new SimpleValve("post-2"), new SimpleValve("post-3") });
        pipelinesMap.put("basic-pipeline", basicPipeline);
        
        HstSitePipeline interruptingPipeline = new HstSitePipeline();
        interruptingPipeline.setPreInvokingValves(new Valve [] { new SimpleValve("pre-1"), new PipelineCompletingValve("interrupt-1"), new SimpleValve("pre-2"), new SimpleValve("pre-3") });
        interruptingPipeline.setInvokingValves(new Valve [] { new SimpleValve("main-1"), new SimpleValve("main-2"), new SimpleValve("main-3") });
        interruptingPipeline.setPostInvokingValves(new Valve [] { new SimpleValve("post-1"), new SimpleValve("post-2"), new SimpleValve("post-3") });
        pipelinesMap.put("interrupting-pipeline", interruptingPipeline);
        
        HstSitePipeline interruptingPipeline2 = new HstSitePipeline();
        interruptingPipeline2.setPreInvokingValves(new Valve [] { new SimpleValve("pre-1"), new SimpleValve("pre-2"), new PipelineCompletingValveWithIgnoredInvokeNext("interrupt-2"), new SimpleValve("pre-3") });
        interruptingPipeline2.setInvokingValves(new Valve [] { new SimpleValve("main-1"), new SimpleValve("main-2"), new SimpleValve("main-3") });
        interruptingPipeline2.setPostInvokingValves(new Valve [] { new SimpleValve("post-1"), new SimpleValve("post-2"), new SimpleValve("post-3") });
        pipelinesMap.put("interrupting-pipeline-2", interruptingPipeline2);

        HstSitePipeline interruptingPipeline3 = new HstSitePipeline();
        interruptingPipeline3.setPreInvokingValves(new Valve [] { new SimpleValve("pre-1"), new SimpleValve("pre-2"), new SimpleValve("pre-3") });
        interruptingPipeline3.setInvokingValves(new Valve [] { new SimpleValve("main-1"), new SimpleValve("main-2"),  new PipelineCompletingValve("interrupt-3"), new SimpleValve("main-3") });
        interruptingPipeline3.setPostInvokingValves(new Valve [] { new SimpleValve("post-1"), new SimpleValve("post-2"), new SimpleValve("post-3") });
        pipelinesMap.put("interrupting-pipeline-3", interruptingPipeline3);

        HstSitePipeline interruptingPipeline4 = new HstSitePipeline();
        interruptingPipeline4.setPreInvokingValves(new Valve [] { new SimpleValve("pre-1"), new SimpleValve("pre-2"), new SimpleValve("pre-3") });
        interruptingPipeline4.setInvokingValves(new Valve [] { new SimpleValve("main-1"), new SimpleValve("main-2"), new SimpleValve("main-3") });
        interruptingPipeline4.setPostInvokingValves(new Valve [] { new SimpleValve("post-1"),  new PipelineCompletingValve("interrupt-4"), new SimpleValve("post-2"), new SimpleValve("post-3") });
        pipelinesMap.put("interrupting-pipeline-4", interruptingPipeline4);

        HstSitePipelines pipelines = new HstSitePipelines();
        pipelines.setPipelines(pipelinesMap);
        
        requestProcessor = new HstRequestProcessorImpl(pipelines);
        requestContainerConfig = new HstContainerConfigImpl(new MockServletContext(), getClass().getClassLoader());
    }
    
    @Test
    public void testBasicPipeline() throws Exception {
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpServletResponse servletResponse = new MockHttpServletResponse();
        HstRequestContext requestContext = new MockHstRequestContext();
        
        requestProcessor.processRequest(requestContainerConfig, requestContext, servletRequest, servletResponse, "basic-pipeline");
        
        log.info("request attribute names: " + EnumerationUtils.toList(servletRequest.getAttributeNames()));
        
        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("pre-" + i));
            assertTrue((Boolean) servletRequest.getAttribute("main-" + i));
            assertTrue((Boolean) servletRequest.getAttribute("post-" + i));
        }
    }
    
    @Test
    public void testCompletePipeline() throws Exception {
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpServletResponse servletResponse = new MockHttpServletResponse();
        HstRequestContext requestContext = new MockHstRequestContext();
        
        requestProcessor.processRequest(requestContainerConfig, requestContext, servletRequest, servletResponse, "interrupting-pipeline");

        log.info("request attribute names: " + EnumerationUtils.toList(servletRequest.getAttributeNames()));

        assertTrue((Boolean) servletRequest.getAttribute("pre-1"));
        assertTrue((Boolean) servletRequest.getAttribute("interrupt-1"));
        
        for (int i = 2; i <= 3; i++) {
            assertNull(servletRequest.getAttribute("pre-" + i));
        }
        
        for (int i = 1; i <= 3; i++) {
            assertNull(servletRequest.getAttribute("main-" + i));
        }
        
        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("post-" + i));
        }
    }
    
    @Test
    public void testCompletePipelineWithIgnoredInvokeNext() throws Exception {
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpServletResponse servletResponse = new MockHttpServletResponse();
        HstRequestContext requestContext = new MockHstRequestContext();
        
        requestProcessor.processRequest(requestContainerConfig, requestContext, servletRequest, servletResponse, "interrupting-pipeline-2");

        log.info("request attribute names: " + EnumerationUtils.toList(servletRequest.getAttributeNames()));

        assertTrue((Boolean) servletRequest.getAttribute("pre-1"));
        assertTrue((Boolean) servletRequest.getAttribute("pre-2"));
        assertTrue((Boolean) servletRequest.getAttribute("interrupt-2"));
        assertNull(servletRequest.getAttribute("pre-3"));
        
        for (int i = 1; i <= 3; i++) {
            assertNull(servletRequest.getAttribute("main-" + i));
        }
        
        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("post-" + i));
        }
    }
    
    @Test
    public void testInterruptionInMainValves() throws Exception {
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpServletResponse servletResponse = new MockHttpServletResponse();
        HstRequestContext requestContext = new MockHstRequestContext();
        
        requestProcessor.processRequest(requestContainerConfig, requestContext, servletRequest, servletResponse, "interrupting-pipeline-3");

        log.info("request attribute names: " + EnumerationUtils.toList(servletRequest.getAttributeNames()));

        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("pre-" + i));
        }
        
        assertTrue((Boolean) servletRequest.getAttribute("main-1"));
        assertTrue((Boolean) servletRequest.getAttribute("main-2"));
        assertTrue((Boolean) servletRequest.getAttribute("interrupt-3"));
        assertNull(servletRequest.getAttribute("main-3"));
        
        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("post-" + i));
        }
    }
    
    @Test
    public void testInterruptionInPostValves() throws Exception {
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpServletResponse servletResponse = new MockHttpServletResponse();
        HstRequestContext requestContext = new MockHstRequestContext();
        
        requestProcessor.processRequest(requestContainerConfig, requestContext, servletRequest, servletResponse, "interrupting-pipeline-4");

        log.info("request attribute names: " + EnumerationUtils.toList(servletRequest.getAttributeNames()));

        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("pre-" + i));
        }
        
        for (int i = 1; i <= 3; i++) {
            assertTrue((Boolean) servletRequest.getAttribute("main-" + i));
        }
        
        assertTrue((Boolean) servletRequest.getAttribute("post-1"));
        assertTrue((Boolean) servletRequest.getAttribute("interrupt-4"));
        assertNull(servletRequest.getAttribute("post-2"));
        assertNull(servletRequest.getAttribute("post-3"));
    }
    
    private static class SimpleValve implements Valve {
        
        private String name;
        
        private SimpleValve(String name) {
            this.name = name;
        }
        
        public void initialize() throws ContainerException {
        }
        
        public void destroy() {
        }
        
        public void invoke(ValveContext context) throws ContainerException {
            setNameAttribute(context);
            context.invokeNext();
        }
        
        protected void setNameAttribute(ValveContext context) {
            context.getServletRequest().setAttribute(name, Boolean.TRUE);
        }
        
    }
    
    private static class PipelineCompletingValve extends SimpleValve {
        
        private PipelineCompletingValve(String name) {
            super(name);
        }
        
        @Override
        public void invoke(ValveContext context) throws ContainerException {
            setNameAttribute(context);
            context.completePipeline();
        }
        
    }
    
    private static class PipelineCompletingValveWithIgnoredInvokeNext extends SimpleValve {
        
        private PipelineCompletingValveWithIgnoredInvokeNext(String name) {
            super(name);
        }
        
        @Override
        public void invoke(ValveContext context) throws ContainerException {
            setNameAttribute(context);
            context.completePipeline();
            context.invokeNext();
        }
        
    }
}
