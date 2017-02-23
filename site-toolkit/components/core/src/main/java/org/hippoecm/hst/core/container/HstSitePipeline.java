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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.order.ObjectOrderer;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstSitePipeline
 *
 */
public class HstSitePipeline implements Pipeline
{

    protected final static Logger log = LoggerFactory.getLogger(HstSitePipeline.class);
    
    protected Valve [] initializationValves;
    protected Valve [] processingValves;
    protected Valve [] cleanupValves;
    
    private Valve [] mergedProcessingValves;
    private Valve [] mergedCleanupValves;
    
    public HstSitePipeline() throws Exception
    {
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
     */
    public void addInitializationValve(Valve initializationValve) {
        initializationValves = add(initializationValves, initializationValve);
        mergedProcessingValves = null;
    }

    public void setProcessingValves(Valve [] processingValves) {
        if (processingValves == null) {
            this.processingValves = null;
        } else {
            this.processingValves = new Valve[processingValves.length];
            System.arraycopy(processingValves, 0,
                    this.processingValves, 0, processingValves.length);
        }
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
        mergedCleanupValves = null;
    }

    
    /*
     * 
     */
    public void addCleanupValve(Valve cleanupValve) {
        cleanupValves = add(cleanupValves, cleanupValve);
        mergedCleanupValves = null;
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
            mergedProcessingValves = mergeProcessingValves();

            if (log.isInfoEnabled()) {
                log.info("mergedProcessingValves:\n\t{}", StringUtils.join(mergedProcessingValves, "\n\t"));
            }
        }

        invokeValves(requestContainerConfig, requestContext, mergedProcessingValves, true);
    }

    public void cleanup(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        if (mergedCleanupValves == null) {
            mergedCleanupValves = mergeCleanupValves();

            if (log.isInfoEnabled()) {
                log.info("mergedCleanupValves:\n\t{}", StringUtils.join(mergedCleanupValves, "\n\t"));
            }
        }

        invokeValves(requestContainerConfig, requestContext, mergedCleanupValves, false);
    }
    
    private void invokeValves(final HstContainerConfig requestContainerConfig, final HstRequestContext requestContext,
                              final Valve [] valves, final boolean withDiagnostics) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(requestContainerConfig, requestContext, valves, withDiagnostics).invokeNext();
        }
    }

    public void destroy() throws ContainerException {
    }

    protected Valve[] mergeProcessingValves() {
        ObjectOrderer<Valve> orderer = new ObjectOrderer<Valve>("initializationValves");

        if (initializationValves != null) {
            addValvesIntoObjectOrdererWithImposingImplicitOrdering(orderer, initializationValves);
        }

        Valve [] orderedInitializationValves = orderer.getOrderedObjects().toArray(new Valve[orderer.getOrderedObjects().size()]);

        orderer = new ObjectOrderer<Valve>("processingValves");

        if (processingValves != null) {
            addValvesIntoObjectOrdererWithImposingImplicitOrdering(orderer, processingValves);
        }

        Valve [] orderedProcessingValves = orderer.getOrderedObjects().toArray(new Valve[orderer.getOrderedObjects().size()]);
        return (Valve[]) ArrayUtils.addAll(orderedInitializationValves, orderedProcessingValves);
    }

    protected Valve[] mergeCleanupValves() {
        ObjectOrderer<Valve> orderer = new ObjectOrderer<Valve>("cleanupValves");

        if (cleanupValves != null) {
            addValvesIntoObjectOrdererWithImposingImplicitOrdering(orderer, cleanupValves);
        }

        return orderer.getOrderedObjects().toArray(new Valve[orderer.getOrderedObjects().size()]);
    }

    /**
     * Adding valves with imposing implicit valves ordering makes sure that adding a new valve with before and after constrainst can *never*
     * reshuffle existing valves. Thus for example, if you already have the valves a,b,c,d,e in that order, then, adding a
     * valve 'f' before c and after a can never change the relative order of the already present valves
     */
    private void addValvesIntoObjectOrdererWithImposingImplicitOrdering(ObjectOrderer<Valve> valveOrderer, final Valve [] valves) {
        String prevOrderableValveName = null;

        for (Valve valve : valves) {
            final String valveName;
            String afterValves = null;
            String beforeValves = null;

            if (valve instanceof OrderableValve) {
                OrderableValve orderableValve = (OrderableValve) valve;
                valveName =  StringUtils.defaultIfEmpty(orderableValve.getValveName(), valve.toString());

                if (StringUtils.isNotEmpty(orderableValve.getAfterValves())) {
                    afterValves = orderableValve.getAfterValves();
                }

                if (StringUtils.isNotEmpty(orderableValve.getBeforeValves())) {
                    beforeValves = orderableValve.getBeforeValves();
                }
            } else {
                valveName = valve.toString();
            }

            if (prevOrderableValveName != null && afterValves == null && beforeValves == null) {
                // imply implicit ordering
                afterValves = prevOrderableValveName;
            }

            valveOrderer.add(valve, valveName, afterValves, beforeValves);

            prevOrderableValveName = valveName;
        }
    }

    static final class Invocation implements ValveContext
    {

        private final Valve[] valves;
        private boolean withDiagnostics;

        private final HstContainerConfig requestContainerConfig;
        private HstComponentWindow rootComponentWindow;
        private HstComponentWindow rootComponentRenderingWindow;
        private final HstRequestContext requestContext;
        private final PageCacheContext pageCacheContext = new PageCacheContextImpl();

        private int at = 0;
        private Task pipelineTask;

        public Invocation(final HstContainerConfig requestContainerConfig, final HstRequestContext requestContext,
                          final Valve[] valves) {
            this(requestContainerConfig, requestContext, valves, false);
        }

        public Invocation(final HstContainerConfig requestContainerConfig, final HstRequestContext requestContext,
                          final Valve[] valves, final boolean withDiagnostics) {
            this.requestContainerConfig = requestContainerConfig;
            this.requestContext = requestContext;
            this.valves = valves;
            this.withDiagnostics = withDiagnostics;
        }

        public void invokeNext() throws ContainerException {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                try {
                    if (HDC.isStarted() && withDiagnostics) {
                        if (pipelineTask != null) {
                            pipelineTask.stop();
                        }
                        pipelineTask = HDC.getCurrentTask().startSubtask("Invoke Valve " +next.getClass().getName());
                    }
                    next.invoke(this);
                } finally {
                    if (pipelineTask != null) {
                        pipelineTask.stop();
                        pipelineTask = null;
                    }
                }
            }
        }

        public HstContainerConfig getRequestContainerConfig() {
            return requestContainerConfig;
        }
        
        public HstRequestContext getRequestContext() {
            return requestContext;
        }
        public HttpServletRequest getServletRequest() {
            return requestContext.getServletRequest();
        }

        public HttpServletResponse getServletResponse() {
            return requestContext.getServletResponse();
        }

        /**
         * @deprecated deprecated since 2.26.02. Use {@link org.hippoecm.hst.core.internal.HstMutableRequestContext#setServletResponse(javax.servlet.http.HttpServletResponse)} instead
         */
        @Deprecated
        public void setHttpServletResponse(HttpServletResponse servletResponse) {
            ((HstMutableRequestContext)requestContext).setServletResponse(servletResponse);
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

        @Override
        public PageCacheContext getPageCacheContext() {
            return pageCacheContext;
        }
    }
    
    private final static class PageCacheContextImpl implements PageCacheContext {

        private final PageCacheKey pageCacheKey = new PageCacheKeyImpl();
        private boolean cacheable = true;
        private List<String> reasonsUncacheable = new ArrayList<String>();

        @Override
        public boolean isCacheable() {
            return cacheable;
        }

        @Override
        public void markUncacheable() {
            cacheable = false;
        }

        @Override
        public void markUncacheable(String reasonUncacheable) {
            cacheable = false;
            reasonsUncacheable.add(reasonUncacheable);
        }

        @Override
          public List<String> getReasonsUncacheable() {
            return reasonsUncacheable;
        }

        @Override
        public PageCacheKey getPageCacheKey() {
            return pageCacheKey;
        }
    }
    
    private final static class PageCacheKeyImpl implements PageCacheKey {

        private Map<String, Serializable> linkedKeyFragments = new LinkedHashMap<String, Serializable>();
        // we keep the hashcode as instance variable for efficiency
        private int hashCode;
        private boolean sealed;

        @Override
        public void setAttribute(final String subKey, final Serializable keyFragment) {
            if (sealed) {
                throw new IllegalStateException("PageCacheKey has been sealed already.");
            }
            linkedKeyFragments.put(subKey, keyFragment);
            hashCode = 0;
        }

        @Override
        public void seal() {
            sealed = true;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PageCacheKeyImpl)) {
                return false;
            }
            final PageCacheKeyImpl cacheKey = (PageCacheKeyImpl) o;
            if (hashCode() != cacheKey.hashCode()) {
                return false;
            }

            // we *cannot* use LinkedHashMap equals impl here because the Cache key equals SHOULD involve
            // the ORDER of the entries. Two LinkedHashMap's that contain the same entries in a different order
            // return TRUE, which we do not want! For the cachekey, the ORDER DOES MATTER

            final Set<Map.Entry<String,Serializable>> entries = linkedKeyFragments.entrySet();
            final Set<Map.Entry<String,Serializable>> otherEntries = ((PageCacheKeyImpl)o).linkedKeyFragments.entrySet();

            if (entries.size() != otherEntries.size()) {
                return false;
            }

            final Iterator<Map.Entry<String, Serializable>> iterator = entries.iterator();
            final Iterator<Map.Entry<String, Serializable>> otherIterator = otherEntries.iterator();

            while (iterator.hasNext()) {
                final Map.Entry<String, Serializable> next = iterator.next();
                final Map.Entry<String, Serializable> otherNext = otherIterator.next();
                if (!next.equals(otherNext)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = linkedKeyFragments.hashCode();
                result = 31 * result + (sealed ? 1 : 0);
                hashCode = result;
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "PageCacheKey[" + linkedKeyFragments.toString() + ", sealed = " +sealed + "]";
        }
    }
}
