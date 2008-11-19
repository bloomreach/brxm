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
package org.hippoecm.hst.core.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLBaseHttpRequestServletWrapper extends HttpServletRequestWrapper {
	private static final Logger log = LoggerFactory.getLogger(URLBaseHttpRequestServletWrapper.class);
    private String urlPrefix;
    private String strippedRequestURI;
	
	
	public URLBaseHttpRequestServletWrapper(HttpServletRequest request, String urlPrefix) {
		super(request);	
		this.urlPrefix = urlPrefix;
		
		//strip contextpath from requestURI
		strippedRequestURI = request.getRequestURI().replaceFirst(request.getContextPath(), "");
		//strip urlprefix from uri
		strippedRequestURI = strippedRequestURI.replaceFirst(urlPrefix, "");
	}

	@Override
	public String getRequestURI() {
		log.debug("getRequestURI()=" + super.getRequestURI() + " returns " + strippedRequestURI);
		return UrlUtilities.decodeUrl(strippedRequestURI);

	}
 
	@Override
	public StringBuffer getRequestURL() {
		//return new StringBuffer(stripUrlPrefix(super.getRequestURL().toString()));
		return super.getRequestURL();
	}

	@Override
	public String getServletPath() {
		return stripUrlPrefix(super.getServletPath());
	}
	
	private String stripUrlPrefix(String original) {
		String result = original.replaceFirst(urlPrefix, "");
		return result;
	}
	
}
