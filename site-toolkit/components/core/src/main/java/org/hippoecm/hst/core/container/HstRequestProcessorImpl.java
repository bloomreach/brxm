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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class HstRequestProcessorImpl implements HstRequestProcessor {
    
    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        processRequest(requestContainerConfig, servletRequest, servletResponse, null);
    }
    
    public void processRequest(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, String pathInfo) throws ContainerException {
        // this request processor's classloader could be different from the above classloader
        // because this request processor and other components could be loaded from another web application context
        // such as a portal web application.
        //
        // If the container's classloader is different from the request processor's classloader,
        // then the container's classloader should be switched into the request processor's classloader
        // because some components like HippoRepository component can be dependent
        // on some interfaces/classes of its own classloader.
        
        ClassLoader containerClassLoader = requestContainerConfig.getContextClassLoader();
        ClassLoader processorClassLoader = getClass().getClassLoader();

        Pipeline pipeline = this.pipelines.getDefaultPipeline();
        
        try {
            if (processorClassLoader != containerClassLoader) {
                Thread.currentThread().setContextClassLoader(processorClassLoader);
            }
            
            if (pathInfo != null) {
                servletRequest.setAttribute(ContainerConstants.HST_CONTAINER_PATH_INFO, pathInfo);
            }
            
            pipeline.beforeInvoke(requestContainerConfig, servletRequest, servletResponse);
            pipeline.invoke(requestContainerConfig, servletRequest, servletResponse);
        } catch(ContainerNotFoundException e){
          throw e;      
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            pipeline.afterInvoke(requestContainerConfig, servletRequest, servletResponse);
            
            if (pathInfo != null) {
                servletRequest.removeAttribute(ContainerConstants.HST_CONTAINER_PATH_INFO);
            }
            
            if (processorClassLoader != containerClassLoader) {
                Thread.currentThread().setContextClassLoader(containerClassLoader);
            }
        }
    }
    
}
