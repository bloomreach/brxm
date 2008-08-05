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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextBaseFilter extends HstFilterBase implements Filter {
	private static final Logger log = LoggerFactory.getLogger(ContextBaseFilter.class);
	public static final String URLBASE_INIT_PARAMETER = "urlBase";
	public static final String ATTRIBUTENAME_INIT_PARAMETER = "attributeName";
	public static final String REPOSITORYLOCATION_INIT_PARAMETER = "repositoryLocation";
	
	private String urlPrefix;
	private String contextBasePath;
	private String requestAttributeName;
	
	public void init(FilterConfig filterConfig) throws ServletException {
		urlPrefix = getInitParameter(filterConfig, URLBASE_INIT_PARAMETER);
		contextBasePath = getInitParameter(filterConfig, REPOSITORYLOCATION_INIT_PARAMETER);
		requestAttributeName = getInitParameter(filterConfig, ATTRIBUTENAME_INIT_PARAMETER);		
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
		int idx = original.indexOf(urlPrefix);
		String result = original.replaceFirst(urlPrefix, "");
		log.info("strip result=" + result);
		return result;
	}
	
	
	
	
	
	
	
}
