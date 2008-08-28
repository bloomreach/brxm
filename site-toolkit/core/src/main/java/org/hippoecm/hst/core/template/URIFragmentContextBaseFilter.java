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
import org.hippoecm.hst.core.mapping.URLMappingImpl;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This filter initializes both the content ContextBase and the ContextBase that refers to the hst:configuration
 * location. Both contextBases are made available on the request as attributes.<br/>
 * 
 * The content ContextBase is determines the root of the content in the repository from a prefix of the 
 * requestURI.
 * 
 * 
 * </p>
 *
 */
public class URIFragmentContextBaseFilter  extends HstFilterBase implements Filter {
	private static final Logger log = LoggerFactory.getLogger(URIFragmentContextBaseFilter.class);
	
	private static final String CONTENT_BASE_INIT_PARAMETER = "contentBase";
	private static final String URI_LEVEL_INIT_PARAMETER = "levels";
	private static final String RELATIVE_HST_CONFIGURATION_LOCATION = "/hst:configuration/hst:configuration";	
	
	private String contentBase;
	private int uriLevels;

	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		contentBase = ContextBase.stripFirstSlash(getInitParameter(filterConfig, CONTENT_BASE_INIT_PARAMETER, true));
		try {
			uriLevels =  Integer.parseInt(getInitParameter(filterConfig, URI_LEVEL_INIT_PARAMETER, true));
		} catch (NumberFormatException e) {
			throw new ServletException("The init-parameter " + URI_LEVEL_INIT_PARAMETER + " is not an int.");
		}
	}
	
	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
		
		
		if (ignorePath(request)) {
			
			filterChain.doFilter(request, response);			
		} else {
			String requestURI = request.getRequestURI().replaceFirst(request.getContextPath(), "");
			log.debug("requestURI: " + requestURI);
			String uriPrefix = getLevelPrefix(requestURI, uriLevels);
			log.debug("uriPrefix: " + uriPrefix);
			
			long start = System.nanoTime();
            URLMappingImpl urlMapping = new URLMappingImpl(JCRConnectorWrapper.getJCRSession(request.getSession()), request.getContextPath() ,uriPrefix ,contentBase + uriPrefix + RELATIVE_HST_CONFIGURATION_LOCATION );
           
            log.debug("creating mapping took " + (System.nanoTime() - start) / 1000000 + " ms.");
            request.setAttribute(HSTHttpAttributes.URL_MAPPING_ATTR, urlMapping);
            
			//content configuration contextbase
			ContextBase contentContextBase = null;
			try {
				contentContextBase = new ContextBase(uriPrefix, contentBase + uriPrefix, request);
			} catch (PathNotFoundException e) {
				throw new ServletException(e);
			} catch (RepositoryException e) {
				throw new ServletException(e);
			}
			
			//hst configuration contextbase
			ContextBase hstConfigurationContextBase = null;
			try {
				hstConfigurationContextBase = new ContextBase(TEMPLATE_CONTEXTBASE_NAME, contentBase + uriPrefix + RELATIVE_HST_CONFIGURATION_LOCATION, request);
			} catch (PathNotFoundException e) {
				throw new ServletException(e);
			} catch (RepositoryException e) {
				throw new ServletException(e);
			}
			
			HttpServletRequestWrapper prefixStrippedRequest = new URLBaseHttpRequestServletWrapper(request, uriPrefix);
           
			prefixStrippedRequest.setAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE, contentContextBase);
		    prefixStrippedRequest.setAttribute(HSTHttpAttributes.CURRENT_HSTCONFIGURATION_CONTEXTBASE_REQ_ATTRIBUTE, hstConfigurationContextBase);
		    prefixStrippedRequest.setAttribute(HSTHttpAttributes.ORIGINAL_REQUEST_URI_REQ_ATTRIBUTE, requestURI);
		    prefixStrippedRequest.setAttribute(HSTHttpAttributes.URI_PREFIX_REQ_ATTRIBUTE, uriPrefix);
		    
			filterChain.doFilter(prefixStrippedRequest, response);			
		
	   }
		
	}
	
	/**
	 * Returns the prefix of a String where the prefix has a specified number of
	 * slashes.
	 * @param requestURI
	 * @param levels
	 * @return
	 */
	private String getLevelPrefix(String requestURI, int levels) {
		String [] splittedURI = requestURI.split("/");
		if (splittedURI.length <= levels) {
			return null;
		}
		StringBuffer levelPrefix = new StringBuffer();
		for (int i=1; i <= levels; i++) {
			levelPrefix.append("/").append(splittedURI[i]);
		}
		return levelPrefix.toString();
	}
}






