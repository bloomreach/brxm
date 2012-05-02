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

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstSitePipeline
 * 
 * @version $Id$
 */
public class HstSitePipeline implements Pipeline
{

    protected final static Logger log = LoggerFactory.getLogger(HstSitePipeline.class);
    
    protected Valve [] initializationValves;
    protected Valve [] processingValves;
    protected Valve [] cleanupValves;
    
    private Valve [] mergedProcessingValves;
    
    public HstSitePipeline() throws Exception
    {
    }

    /**
     * @param initializationValves
     * @deprecated use {@link #setInitializationValves(Valve[])} instead
     */
    @Deprecated
    public void setPreInvokingValves(Valve [] initializationValves) {
        log.warn("preInvokingValves is deprecated. Use initializationValves instead");
        if (initializationValves == null) {
            this.initializationValves = null;
        } else {
            this.initializationValves = new Valve[initializationValves.length];
            System.arraycopy(initializationValves, 0, this.initializationValves, 0, initializationValves.length);
        }
        mergedProcessingValves = null;
    }
    
    /**
     * 
     * @param initializationValves
     */
    public void setInitializationValves(Valve [] initializationValves) {
        if (initializationValves == null) {
            this.initializationValves = null;
        } else {
            this.initializationValves = new Valve[initializationValves.length];
            System.arraycopy(initializationValves, 0, this.initializationValves, 0, initializationValves.length);
        }
        mergedProcessingValves = null;
    }
    
    /**
     * @param initializationValve
     * @deprecated use {@link #addInitializationValve(Valve)} instead
     */
    @Deprecated
    public void addPreInvokingValve(Valve initializationValve) {
        log.warn("addPreInvokingValve is deprecated. Use addInitializationValve instead");
        initializationValves = add(initializationValves, initializationValve);
        mergedProcessingValves = null;
    }
    
    /**
     * @param initializationValve
     */
    public void addInitializationValve(Valve initializationValve) {
        initializationValves = add(initializationValves, initializationValve);
        mergedProcessingValves = null;
    }
    
    /**
     * @param processingValves
     * @deprecated use {@link #setProcessingValves(Valve[])} instead
     */
    @Deprecated
    public void setInvokingValves(Valve [] processingValves) {
        log.warn("invokingValves is deprecated. Use processingValves instead");
        if (processingValves == null) {
            this.processingValves = null;
        } else {
            this.processingValves = new Valve[processingValves.length];
            System.arraycopy(processingValves, 0, this.processingValves, 0, processingValves.length);
        }
        mergedProcessingValves = null;
    }
    
    /**
     * 
     * @param processingValves
     */
    public void setProcessingValves(Valve [] processingValves) {
        if (processingValves == null) {
            this.processingValves = null;
        } else {
            this.processingValves = new Valve[processingValves.length];
            System.arraycopy(processingValves, 0, this.processingValves, 0, processingValves.length);
        }
        mergedProcessingValves = null;
    }
    
    /**
     * @param processingValve
     * @deprecated use {@link #addProcessingValve(Valve)} instead
     */
    @Deprecated
    public void addInvokingValve(Valve processingValve) {
        log.warn("addInvokingValve is deprecated. Use addProcessingValve instead");
        processingValves = add(processingValves, processingValve);
        mergedProcessingValves = null;
    }
    
    /**
     * 
     * @param processingValve
     */
    public void addProcessingValve(Valve processingValve) {
        processingValves = add(processingValves, processingValve);
        mergedProcessingValves = null;
    }
    
    /**
     * @param cleanupValves
     * @deprecated use {@link #setCleanupValves(Valve[])} instead
     */
    @Deprecated
    public void setPostInvokingValves(Valve [] cleanupValves) {
        log.warn("postInvokingValves is deprecated. Use cleanupValves instead");
        if (cleanupValves == null) {
            this.cleanupValves = null;
        } else {
            this.cleanupValves = new Valve[cleanupValves.length];
            System.arraycopy(cleanupValves, 0, this.cleanupValves, 0, cleanupValves.length);
        }
    }
    
    /**
     * 
     * @param cleanupValve
     */
    public void setCleanupValves(Valve [] cleanupValve) {
        if (cleanupValve == null) {
            this.cleanupValves = null;
        } else {
            this.cleanupValves = new Valve[cleanupValve.length];
            System.arraycopy(cleanupValve, 0, this.cleanupValves, 0, cleanupValve.length);
        }
    }

    
    /*
     * 
     */
    public void addCleanupValve(Valve cleanupValve) {
        cleanupValves = add(cleanupValves, cleanupValve);
    }

    private Valve[] add(Valve[] valves, Valve valve) {
        if(valve == null) {
            
            return valves;
        }
        Valve[] newValves;
        if (valves == null) {
            newValves = new Valve[1];
            newValves[0] = valve;
        } else {
            newValves =  new Valve[valves.length +1];
            System.arraycopy(valves, 0, newValves, 0, valves.length);
            newValves[newValves.length -1] = valve;
        }
        return newValves;
    }
    
    
    public void initialize() throws ContainerException {
    }


    public void invoke(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        if (mergedProcessingValves == null) {
            mergedProcessingValves = (Valve[]) ArrayUtils.addAll(initializationValves, processingValves);
        }
        
        invokeValves(requestContainerConfig, requestContext, servletRequest, servletResponse, mergedProcessingValves);
    }

    public void cleanup(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, requestContext, servletRequest, servletResponse, cleanupValves);
    }
    
    private void invokeValves(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve [] valves) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(requestContainerConfig, requestContext, servletRequest, servletResponse, valves).invokeNext();
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
        private HstComponentWindow rootComponentRenderingWindow;
        private final HstRequestContext requestContext;

        private int at = 0;

        public Invocation(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve[] valves) {
            this.requestContainerConfig = requestContainerConfig;
            this.requestContext = requestContext;
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
        
        public HstRequestContext getRequestContext() {
            return this.requestContext;
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
        
        
        public void setRootComponentRenderingWindow(HstComponentWindow rootComponentRenderingWindow) {
            this.rootComponentRenderingWindow = rootComponentRenderingWindow;
        }
        /**
         * returns the rootComponentRenderingWindow and when it is <code>null</code> it returns the default
         * rootComponentWindow
         */
        public HstComponentWindow getRootComponentRenderingWindow() {
            return rootComponentRenderingWindow == null ? rootComponentWindow : rootComponentRenderingWindow;
        }
    }
}
