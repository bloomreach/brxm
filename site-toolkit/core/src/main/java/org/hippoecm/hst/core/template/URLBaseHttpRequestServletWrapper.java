package org.hippoecm.hst.core.template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

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
