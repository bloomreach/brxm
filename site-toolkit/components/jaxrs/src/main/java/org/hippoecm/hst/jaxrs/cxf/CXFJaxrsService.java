/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.cxf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.ServletTransportFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.AbstractJaxrsService;

/**
 * @version $Id$
 *
 */
public class CXFJaxrsService extends AbstractJaxrsService {

	private static final String SERVLET_CONTROLLER_ATTRIBUTE_NAME_PREFIX = CXFJaxrsService.class.getName() +".ServletController.";
	private static final String CXF_BUS_ATTRIBUTE_NAME_PREFIX = CXFJaxrsService.class.getName() +".CXFBus.";
	private String servletControllerAttributeName;
	private String cxfBusAttributeName;
    private JAXRSServerFactoryBean jaxrsServerFactoryBean;
    
    private List<Interceptor<? extends Message>> inInterceptors;
    private List<Interceptor<? extends Message>> inFaultInterceptors;
    private List<Interceptor<? extends Message>> outInterceptors;
    private List<Interceptor<? extends Message>> outFaultInterceptors;
	
    public CXFJaxrsService(String serviceName) {
    	this(serviceName, new HashMap<String,String>());
    }
    
    public CXFJaxrsService(String serviceName, Map<String, String> jaxrsConfigParameters) {
    	super(serviceName, jaxrsConfigParameters);
    	this.servletControllerAttributeName = SERVLET_CONTROLLER_ATTRIBUTE_NAME_PREFIX + serviceName;
    	this.cxfBusAttributeName = CXF_BUS_ATTRIBUTE_NAME_PREFIX + serviceName;
    }
    
	public synchronized void setJaxrsServerFactoryBean(JAXRSServerFactoryBean jaxrsServerFactoryBean) {
		this.jaxrsServerFactoryBean = jaxrsServerFactoryBean;
	}
	
    public void setInInterceptors(List<Interceptor<? extends Message>> inInterceptors) {
        this.inInterceptors = inInterceptors;
    }

    public void setInFaultInterceptors(List<Interceptor<? extends Message>> inFaultInterceptors) {
        this.inFaultInterceptors = inFaultInterceptors;
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> outInterceptors) {
        this.outInterceptors = outInterceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> outFaultInterceptors) {
        this.outFaultInterceptors = outFaultInterceptors;
    }

    protected Bus createBus() {
        BusFactory.setThreadDefaultBus(null);
        Bus bus = BusFactory.getThreadDefaultBus(true);
        
        if (inInterceptors != null && !inInterceptors.isEmpty()) {
            bus.getInInterceptors().addAll(inInterceptors);
        }
        
        if (inFaultInterceptors != null && !inFaultInterceptors.isEmpty()) {
            bus.getInInterceptors().addAll(inFaultInterceptors);
        }
        
        if (outInterceptors != null && !outInterceptors.isEmpty()) {
            bus.getInInterceptors().addAll(outInterceptors);
        }
        
        if (outFaultInterceptors != null && !outFaultInterceptors.isEmpty()) {
            bus.getInInterceptors().addAll(outFaultInterceptors);
        }
        
        return bus;
    }

    protected synchronized ServletController getController(ServletContext servletContext) {
		ServletController controller = (ServletController)servletContext.getAttribute(servletControllerAttributeName);
		if (controller == null) {
			Bus bus = createBus();
			ServletTransportFactory df = new ServletTransportFactory(bus);
			jaxrsServerFactoryBean.setDestinationFactory(df);
			jaxrsServerFactoryBean.create();
			controller = new ServletController(df, getJaxrsServletConfig(servletContext), servletContext, bus);
            // guard against potential concurrency issue in cxf dynamic endpoint state management: HSTTWO-1663, CXF-2997
            controller.setDisableAddressUpdates(true);
			servletContext.setAttribute(servletControllerAttributeName, controller);
			servletContext.setAttribute(cxfBusAttributeName, bus);
		}
		else {
			BusFactory.setThreadDefaultBus((Bus)servletContext.getAttribute(cxfBusAttributeName));
		}
		return controller;
	}
	
	@Override
	public void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException {
		
		try {
			ServletController controller = getController(requestContext.getServletContext());
			HttpServletRequest jaxrsRequest = getJaxrsRequest(requestContext, request); 
			controller.invoke(jaxrsRequest, response);
		} catch (ContainerException e) {
		    throw e;
		} catch (Throwable th) {
			throw new ContainerException(th);
		} finally {
			BusFactory.setThreadDefaultBus(null);
		}
	}
	
    @Override
    protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
        String requestURI = request.getRequestURI();
        HstContainerURL baseURL = requestContext.getBaseURL();
        String pathInfo = StringUtils.substringAfter(requestURI, baseURL.getContextPath() + baseURL.getResolvedMountPath());
        
        if (StringUtils.startsWith(pathInfo, ";")) {
            pathInfo = "/" + StringUtils.substringAfter(pathInfo, "/");
        }
        
        if (StringUtils.isEmpty(pathInfo)) {
            pathInfo = "/";
        }
        
        return pathInfo;
    }
}
