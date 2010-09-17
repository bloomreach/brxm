/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.services.support.jaxrs.cxf;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.ServletTransportFactory;
import org.hippoecm.hst.core.container.JAXRSService;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * @version $Id$
 *
 */
public class CXFJaxrsService implements JAXRSService {

	private static final String SERVLET_CONTROLLER_ATTRIBUTE_NAME_PREFIX = CXFJaxrsService.class.getName() +".ServletController.";
	private static final String CXF_BUS_ATTRIBUTE_NAME_PREFIX = CXFJaxrsService.class.getName() +".CXFBus.";
	private String servletControllerAttributeName;
	private String cxfBusAttributeName;
	private Map<String,String> jaxrsConfigParameters;
	private String serviceName;
	private String basePath = "";
	
    private JAXRSServerFactoryBean jaxrsServerFactoryBean;
	
    public CXFJaxrsService(String serviceName) {
    	this(serviceName, new HashMap<String,String>());
    }
    
    public CXFJaxrsService(String serviceName, Map<String, String> jaxrsConfigParameters) {    	
    	this.serviceName = serviceName;
    	this.servletControllerAttributeName = SERVLET_CONTROLLER_ATTRIBUTE_NAME_PREFIX + serviceName;
    	this.cxfBusAttributeName = CXF_BUS_ATTRIBUTE_NAME_PREFIX + serviceName;
    	this.jaxrsConfigParameters = jaxrsConfigParameters;
    }
    
    public void setServletPath(String servletPath) {
    	this.basePath = servletPath;
    }
    
	public void setJaxrsServerFactoryBean(JAXRSServerFactoryBean jaxrsServerFactoryBean) {
		this.jaxrsServerFactoryBean = jaxrsServerFactoryBean;
	}
	
	protected synchronized ServletController getController(ServletContext servletContext) {
		ServletController controller = (ServletController)servletContext.getAttribute(servletControllerAttributeName);
		if (controller == null) {
			BusFactory.setThreadDefaultBus(null);
			Bus bus = BusFactory.getThreadDefaultBus(true);
			ServletTransportFactory df = new ServletTransportFactory(bus);
			jaxrsServerFactoryBean.setDestinationFactory(df);
			jaxrsServerFactoryBean.create();
			controller = new ServletController(df, new ServletConfigImpl(serviceName, servletContext, jaxrsConfigParameters), 
					servletContext, bus);
			servletContext.setAttribute(servletControllerAttributeName, controller);
			servletContext.setAttribute(cxfBusAttributeName, bus);
		}
		else {
			BusFactory.setThreadDefaultBus((Bus)servletContext.getAttribute(cxfBusAttributeName));
		}
		return controller;
	}
	
	public String getBasePath() {
		return basePath;
	}

	public void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			ServletController controller = getController(requestContext.getServletContext());
			controller.invoke(request, response);
		}
		finally {
			BusFactory.setThreadDefaultBus(null);
		}
	}
	
	private static class ServletConfigImpl implements ServletConfig {
		
		private String servletName;
		private ServletContext context;
		private Map<String,String> initParams;
		
		public ServletConfigImpl(String servletName, ServletContext context, Map<String,String> initParams) {
			this.servletName = servletName;
			this.context = context;
			this.initParams = initParams;
		}

		public String getInitParameter(String name) {
			return initParams.get(name);
		}

		@SuppressWarnings("unchecked")
		public Enumeration getInitParameterNames() {
			return Collections.enumeration(initParams.keySet());
		}

		public ServletContext getServletContext() {
			return context;
		}

		public String getServletName() {
			return servletName;
		}
	}
}
