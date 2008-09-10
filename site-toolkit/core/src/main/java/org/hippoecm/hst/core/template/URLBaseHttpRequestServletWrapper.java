package org.hippoecm.hst.core.template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLBaseHttpRequestServletWrapper extends HttpServletRequestWrapper {
	private static final Logger log = LoggerFactory.getLogger(URLBaseHttpRequestServletWrapper.class);
    private String urlPrefix;
	
	
	public URLBaseHttpRequestServletWrapper(HttpServletRequest request, String urlPrefix) {
		super(request);	
		this.urlPrefix = urlPrefix;
	}

	@Override
	public String getRequestURI() {
		return UrlUtilities.decodeUrl(stripUrlPrefix(super.getRequestURI()));
	}
 
	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(stripUrlPrefix(super.getRequestURL().toString()));
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
