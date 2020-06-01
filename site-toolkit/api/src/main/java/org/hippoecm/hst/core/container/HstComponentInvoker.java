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
package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * HstComponent invoker component. 
 * The HstComponent container invokes HstComponents via this invoker.
 * 
 * @version $Id$
 */
public interface HstComponentInvoker {
    
    /**
     * Invokes the {@link HstComponent#doAction(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokeAction(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    /**
     * Invokes the {@link HstComponent#prepareBeforeRender(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokePrepareBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    /**
     * Invokes the {@link HstComponent#doBeforeRender(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokeBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    /**
     * Dispatches to the <CODE>renderpath</CODE> of the {@link HstComponent}.
     * If the component sets <CODE>renderpath</CODE> dynamically in its
     * {@link HstComponent#doBeforeRender(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method
     * by invoking {@link HstResponse#setRenderPath(String)}, then
     * the HstComponentInvoker will dispatch to the <CODE>renderpath</CODE> which is set by the method.
     * Otherwise, it retrieves the <CODE>renderpath</CODE> from the component configuration to try dispatching.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    /**
     * Invokes the {@link HstComponent#doBeforeServeResource(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokeBeforeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    /**
     * Dispatches to the <CODE>serveresourcepath</CODE> of the {@link HstComponent}.
     * If the component sets <CODE>serveresourcepath</CODE> dynamically in its 
     * {@link HstComponent#doBeforeServeResource(org.hippoecm.hst.core.component.HstRequest, HstResponse)} method
     * by invoking {@link HstResponse#setServeResourcePath(String)}, then
     * the HstComponentInvoker will dispatch to the <CODE>serveresourcepath</CODE> which is set by the method.
     * Otherwise, it retrieves the <CODE>serveresourcepath</CODE> from the component configuration to try dispatching.
     * If it cannot find the configuration in the component configuration, then
     * it will try dispatching to the <CODE>renderpath</CODE> instead.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws ContainerException
     */
    void invokeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
}
