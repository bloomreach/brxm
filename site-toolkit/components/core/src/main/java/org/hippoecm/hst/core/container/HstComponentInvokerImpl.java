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

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    static Log log = LogFactory.getLog(HstComponentInvokerImpl.class);
    
    public void invokeAction(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doAction(hstRequest, hstResponse);
    }

    public void invokeBeforeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeRender(hstRequest, hstResponse);
    }

    public void invokeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletConfig, servletRequest, servletResponse, dispatchUrl);
    }

    public void invokeBeforeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeServeResource(hstRequest, hstResponse);
    }

    public void invokeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletConfig, servletRequest, servletResponse, dispatchUrl);
    }

    protected void invokeDispatcher(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (dispatchUrl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Invoking dispatcher of url: " + dispatchUrl);
            }
            
            if (dispatchUrl.startsWith("/")) {
                disp = servletConfig.getServletContext().getRequestDispatcher(dispatchUrl);
            } else {
                disp = servletConfig.getServletContext().getNamedDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            throw new ContainerException("Cannot create request dispatcher for " + dispatchUrl);
        }
        
        try {
            disp.include(servletRequest, servletResponse);
        } catch (ServletException e) {
            throw new ContainerException(e);
        } catch (IOException e) {
            throw new ContainerException(e);
        }
    }
}
