/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.experiencepage.ExperiencePageLoadingException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_SITE_PIPELINE_NAME;

/**
 * HstRequestProcessorImpl
 * 
 * @version $Id$
 */
public class HstRequestProcessorImpl implements HstRequestProcessor {

    protected final static Logger log = LoggerFactory.getLogger(HstRequestProcessorImpl.class);

    private static ThreadLocal<HstContainerConfig> tlCurrentRequestContainerConfig = new ThreadLocal<HstContainerConfig>();

    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, String namedPipeline) throws ContainerException {
        // this request processor's classloader could be different from the above classloader
        // because this request processor and other components could be loaded from another web application context
        //
        // If the container's classloader is different from the request processor's classloader,
        // then the container's classloader should be switched into the request processor's classloader
        // because some components like HippoRepository component can be dependent
        // on some interfaces/classes of its own classloader.
        
        ClassLoader containerClassLoader = requestContainerConfig.getContextClassLoader();
        ClassLoader processorClassLoader = getClass().getClassLoader();
        
        Pipeline pipeline = (namedPipeline != null ? pipelines.getPipeline(namedPipeline) : pipelines.getDefaultPipeline());
        
        if (pipeline == null) {
        	if (namedPipeline != null) {
            	throw new ContainerException("Unknown namedPipeline "+namedPipeline+". Request processing cannot continue.");
        	}
        	else {
            	throw new ContainerException("No default pipeline defined. Request processing cannot continue.");
        	}
        }
        Task pipelineTask = null;
        try {
            if (HDC.isStarted()) {
                pipelineTask = HDC.getCurrentTask().startSubtask("Pipeline processing");
                pipelineTask.setAttribute("pipeline", (namedPipeline == null) ? DEFAULT_SITE_PIPELINE_NAME : namedPipeline);
            }

            tlCurrentRequestContainerConfig.set(requestContainerConfig);

            if (processorClassLoader != containerClassLoader) {
                Thread.currentThread().setContextClassLoader(processorClassLoader);
            }
       
            pipeline.invoke(requestContainerConfig, requestContext, servletRequest, servletResponse);
            
        } catch (ContainerException | ExperiencePageLoadingException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            if (pipelineTask != null) {
                pipelineTask.stop();
            }
            pipeline.cleanup(requestContainerConfig, requestContext, servletRequest, servletResponse);

            tlCurrentRequestContainerConfig.remove();

            if (processorClassLoader != containerClassLoader) {
                Thread.currentThread().setContextClassLoader(containerClassLoader);
            }
        }
    }

    public HstContainerConfig getCurrentHstContainerConfig() {
        return tlCurrentRequestContainerConfig.get();
    }

}
