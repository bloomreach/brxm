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

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


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
    
    public void beforeInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException {
        invokeValves(servletConfig, servletRequest, servletResponse, preInvokingValves);
    }

    public void invoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException {
        invokeValves(servletConfig, servletRequest, servletResponse, invokingValves);
    }
    
    public void afterInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException {
        invokeValves(servletConfig, servletRequest, servletResponse, postInvokingValves);
    }
    
    private void invokeValves(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse, Valve [] valves) throws ContainerException, ContainerNoMatchException {
        if (valves != null && valves.length > 0) {
            new Invocation(servletConfig, servletRequest, servletResponse, valves).invokeNext();
        }
    }

    public void destroy() throws ContainerException {
    }    
    
    private static final class Invocation implements ValveContext
    {

        private final Valve[] valves;

        private final ServletConfig servletConfig;
        private final ServletRequest servletRequest;
        private final ServletResponse servletResponse;
        private HstComponentWindow rootComponentWindow;

        private int at = 0;

        public Invocation(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse, Valve[] valves) {
            this.servletConfig = servletConfig;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
            this.valves = valves;
        }

        public void invokeNext() throws ContainerException, ContainerNoMatchException {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(this);
            }
        }
        
        public ServletConfig getServletConfig() {
            return this.servletConfig;
        }

        public ServletRequest getServletRequest() {
            return this.servletRequest;
        }

        public ServletResponse getServletResponse() {
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
