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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that works with preview and live content contexts. The filter also configures the location of the
 * template location in the repository.
 *
 */
public class PreviewLiveContextBaseFilter extends HstFilterBase implements Filter {
	private static final Logger log = LoggerFactory.getLogger(PreviewLiveContextBaseFilter.class);
	
	public static final String PREVIEW_REPOSITORYPATH_INIT_PARAMETER = "/content/preview";
	public static final String LIVE_REPOSITORYPATH_INIT_PARAMETER = "/content/live";
	
	public static final String PREVIEW_URL_MATCH__INIT_PARAMETER = "preview-mapping";
	public static final String LIVE_URL_MATCH__INIT_PARAMETER = "live-mapping";
	
	public static final String HST_CONFIGURATION_LOCATION_PARAMETER = "hst-configuation-location";
	
	private String previewRepositoryPath;
	private String liveRepositoryPath;
	private String previewMapping;
	private String liveMapping;
	private String hstConfigurationLocation;
	
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		
		previewRepositoryPath = getInitParameter(filterConfig, PREVIEW_REPOSITORYPATH_INIT_PARAMETER, true);
		liveRepositoryPath = getInitParameter(filterConfig, LIVE_REPOSITORYPATH_INIT_PARAMETER, true);
		previewMapping = getInitParameter(filterConfig, PREVIEW_URL_MATCH__INIT_PARAMETER, true);
		liveMapping = getInitParameter(filterConfig, LIVE_URL_MATCH__INIT_PARAMETER, true);
		hstConfigurationLocation = getInitParameter(filterConfig, HST_CONFIGURATION_LOCATION_PARAMETER, true);
		
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
		request.getRequestURI();
		
		
	}
	
	

}
