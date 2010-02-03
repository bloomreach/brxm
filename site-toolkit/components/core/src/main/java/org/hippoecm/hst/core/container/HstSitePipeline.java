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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HstSitePipeline
 * 
 * @version $Id$
 */
public class HstSitePipeline implements Pipeline
{

    protected Valve [] preInvokingValves;
    protected Valve [] invokingValves;
    protected Valve [] postInvokingValves;
    
    public HstSitePipeline() throws Exception
    {
    }

    public void setPreInvokingValves(Valve [] preInvokingValves) {
        this.preInvokingValves = preInvokingValves;
    }
    
    public void setInvokingValves(Valve [] invokingValves) {
        this.invokingValves = invokingValves;
    }
    
    public void setPostInvokingValves(Valve [] postInvokingValves) {
        this.postInvokingValves = postInvokingValves;
    }

    public void initialize() throws ContainerException {
    }
    
    public void beforeInvoke(HstContainerConfig requestContainerConfig, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, servletRequest, servletResponse, preInvokingValves);
    }

    public void invoke(HstContainerConfig requestContainerConfig, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, servletRequest, servletResponse, invokingValves);
    }
    
    public void afterInvoke(HstContainerConfig requestContainerConfig, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, servletRequest, servletResponse, postInvokingValves);
    }
    
    private void invokeValves(HstContainerConfig requestContainerConfig, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve [] valves) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(requestContainerConfig, servletRequest, servletResponse, valves).invokeNext();
        }
    }

    public void destroy() throws ContainerException {
    }    
    
    private static final class Invocation implements ValveContext
    {

        private final Valve[] valves;

        private final HstContainerConfig requestContainerConfig;
        private final HttpServletRequest servletRequest;
        private final HttpServletResponse servletResponse;
        private HstComponentWindow rootComponentWindow;

        private int at = 0;

        public Invocation(HstContainerConfig requestContainerConfig, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve[] valves) {
            this.requestContainerConfig = requestContainerConfig;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
            this.valves = valves;
        }

        public void invokeNext() throws ContainerException {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(this);
            }
        }

        public HstContainerConfig getRequestContainerConfig() {
            return this.requestContainerConfig;
        }
        
        public HttpServletRequest getServletRequest() {
            return this.servletRequest;
        }

        public HttpServletResponse getServletResponse() {
            return this.servletResponse;
        }

        public void setRootComponentWindow(HstComponentWindow rootComponentWindow) {
            this.rootComponentWindow = rootComponentWindow;
        }
        
        public HstComponentWindow getRootComponentWindow() {
            return this.rootComponentWindow;
        }
    }
}
