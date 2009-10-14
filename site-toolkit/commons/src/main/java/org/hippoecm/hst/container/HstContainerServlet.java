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
package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.site.HstServices;

/**
 * HST Container Servlet
 * 
 * This servlet should serve each request
 * 
 * @version $Id$
 */
public class HstContainerServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    public static final String CONTEXT_NAMESPACE_INIT_PARAM = "hstContextNamespace";
    
    public static final String CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM = "clientComponentManagerClass";
    
    public static final String CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM = "clientComponentManagerConfigurations";

    public static final String CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM = "clientComponentManagerContextAttributeName";

    public static final String CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME = HstContainerServlet.class.getName() + ".clientComponentManager";
    
    public static final String DEFAULT_PIPELINE_INIT_PARAM = "hstDefaultPipeline";
    
    protected HstContainerConfig requestContainerConfig;
    
    protected String contextNamespace;
    
    protected String clientComponentManagerClassName;
    
    protected String [] clientComponentManagerConfigurations;
    
    protected boolean initialized;
    
    protected ComponentManager clientComponentManager;
    
    protected String clientComponentManagerContextAttributeName = HstContainerServlet.class.getName() + ".clientComponentManager";
    
    protected String defaultPipeline;
    
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        
        contextNamespace = getConfigOrContextInitParameter(CONTEXT_NAMESPACE_INIT_PARAM, contextNamespace);
        clientComponentManagerClassName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM, clientComponentManagerClassName);
        
        String param = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM, null);
        
        if (param != null) {
            String [] configs = param.split(",");
            
            for (int i = 0; i < configs.length; i++) {
                configs[i] = configs[i].trim();
            }
            
            clientComponentManagerConfigurations = configs;
        }
        
        clientComponentManagerContextAttributeName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM, clientComponentManagerContextAttributeName);
        defaultPipeline = getConfigOrContextInitParameter(DEFAULT_PIPELINE_INIT_PARAM, null);
        
        initialized = false;
        
        if (HstServices.isAvailable()) {
            doInit(config);
        }
    }
    
    protected synchronized void doInit(ServletConfig config) {
        if (initialized) {
            return;
        }
        
        if (clientComponentManager != null) {
            try {
                clientComponentManager.stop();
                clientComponentManager.close();
            } catch (Exception e) {
            } finally {
                clientComponentManager = null;
            }
        }
        
        try {
            if (clientComponentManagerClassName != null && clientComponentManagerConfigurations != null && clientComponentManagerConfigurations.length > 0) {
                clientComponentManager = (ComponentManager) Thread.currentThread().getContextClassLoader().loadClass(clientComponentManagerClassName).newInstance();
                clientComponentManager.setConfigurationResources(clientComponentManagerConfigurations);
                clientComponentManager.initialize();
                clientComponentManager.start();
                config.getServletContext().setAttribute(clientComponentManagerContextAttributeName, clientComponentManager);
            }
        } catch (Exception e) {
            log("Invalid client component manager class or configuration: " + e);
        } finally {
            initialized = true;
        }
    }
    
    @Override
    public void destroy() {
        initialized = false;

        if (clientComponentManager != null) {
            try{
                clientComponentManager.stop();
                clientComponentManager.close();
            } catch (Exception e) {
            } finally {
                clientComponentManager = null;
            }
        }
    }
    
    // -------------------------------------------------------------------
    // R E Q U E S T P R O C E S S I N G
    // -------------------------------------------------------------------

    /**
     * The primary method invoked when the servlet is executed.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        try {
            if (!HstServices.isAvailable()) {
                String msg = "The HST Container Services are not initialized yet.";
                log(msg);
                res.getWriter().println(msg);
                res.flushBuffer();
                
                return;
            }
            
            if (!initialized) {
                doInit(getServletConfig());
            }
            
            if (this.requestContainerConfig == null) {
                this.requestContainerConfig = new HstContainerConfigImpl(getServletConfig(), Thread.currentThread().getContextClassLoader());
            }
            
            if (this.contextNamespace != null) {
                req.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, contextNamespace);
            }
            
            if (defaultPipeline != null && req.getAttribute(Pipeline.class.getName()) == null) {
                req.setAttribute(Pipeline.class.getName(), defaultPipeline);
            }
            
            HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, req, res);
        } catch (Exception e) {
            final String msg = "Fatal error encountered while processing request: " + e.toString();
            log(msg, e);
            throw new ServletException(msg, e);
        }
    
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    private String getConfigOrContextInitParameter(String paramName, String defaultValue) {
        String value = getServletConfig().getInitParameter(paramName);
        
        if (value == null) {
            value = getServletConfig().getServletContext().getInitParameter(paramName);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return (value != null ? value.trim() : null);
    }
    
}
