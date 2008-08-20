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
package org.hippoecm.hst.core.template;

import java.io.IOException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Web filter that creates a ContextBase instance.
 *
 */
public class ContextBaseFilter extends HstFilterBase implements Filter {
	private static final Logger log = LoggerFactory.getLogger(ContextBaseFilter.class);
	public static final String URLBASE_INIT_PARAMETER = "urlBase";
	public static final String ATTRIBUTENAME_INIT_PARAMETER = "attributeName";
	public static final String REPOSITORYLOCATION_INIT_PARAMETER = "repositoryLocation";
	
	private String urlPrefix;
	private String contextBasePath;
	private String requestAttributeName;
	
	
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		urlPrefix = getInitParameter(filterConfig, URLBASE_INIT_PARAMETER, true);
		contextBasePath = getInitParameter(filterConfig, REPOSITORYLOCATION_INIT_PARAMETER, true);
		requestAttributeName = getInitParameter(filterConfig, ATTRIBUTENAME_INIT_PARAMETER, true);		
	}
	
	public void destroy() {		
	}

	public void doFilter(ServletRequest req, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		log.info("DOFILTER >>>>>>>>");
		HttpServletRequest request = (HttpServletRequest) req;		
		HttpServletRequestWrapper prefixStrippedRequest = new URLBaseHttpRequestServletWrapper(request, urlPrefix);	
		
		ContextBase contextBase;
		try {
			contextBase = new ContextBase(urlPrefix, contextBasePath, request);
		} catch (PathNotFoundException e) {
			throw new ServletException(e);
		} catch (RepositoryException e) {
			throw new ServletException(e);
		}
		
		//
		prefixStrippedRequest.setAttribute(requestAttributeName, contextBase);	
	    prefixStrippedRequest.setAttribute(URLBASE_INIT_PARAMETER, urlPrefix);
		//prefixStrippedRequest.setAttribute(CO, requestAttributeName);
	    prefixStrippedRequest.setAttribute(CONTENT_CONTEXT_REQUEST_ATTRIBUTE, contextBase);
	    prefixStrippedRequest.setAttribute(HSTHttpAttributes.ORIGINAL_REQUEST_URI_REQ_ATTRIBUTE, request.getServletPath());
		
		filterChain.doFilter(prefixStrippedRequest, response);
	}
}

class URLBaseHttpRequestServletWrapper extends HttpServletRequestWrapper {
	private static final Logger log = LoggerFactory.getLogger(URLBaseHttpRequestServletWrapper.class);
    private String urlPrefix;
	
	
	public URLBaseHttpRequestServletWrapper(HttpServletRequest request, String urlPrefix) {
		super(request);	
		this.urlPrefix = urlPrefix;
	}

	@Override
	public String getRequestURI() {
		log.info("getRequestURI()=" + super.getRequestURI());
		return stripUrlPrefix(super.getRequestURI());
	}

	@Override
	public StringBuffer getRequestURL() {
		log.info("getRequestURL()=" + super.getRequestURL());
		return new StringBuffer(stripUrlPrefix(super.getRequestURL().toString()));
	}

	@Override
	public String getServletPath() {
		log.info("getServletPath()=" + super.getServletPath());		
		return stripUrlPrefix(super.getServletPath());
	}
	
	private String stripUrlPrefix(String original) {
		String result = original.replaceFirst(urlPrefix, "");
		log.info("strip result=" + result);
		return result;
	}
	
}
