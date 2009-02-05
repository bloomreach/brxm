package org.hippoecm.hst.core.filters.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstStartRequestFilter implements Filter{


    private static final Logger log = LoggerFactory.getLogger(HstStartRequestFilter.class);
    
    private static final String IGNOREPATHS_FILTER_INIT_PARAM = "ignorePaths"; //comma separated list with ignore path prefixes
    private static final String IGNORETYPES_FILTER_INIT_PARAM = "ignoreTypes"; //comma separated list with ignore path prefixes
    
    private List<String> ignorePathsList = null;
    private List<String> ignoreTypesList = null;
    
    public void destroy() {
        
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        initIgnoreTypes(filterConfig);
        initIgnorePaths(filterConfig);
    }

    
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        HttpServletRequest request = (HttpServletRequest)req;
        String servletCtxStrippedRequestUri = request.getRequestURI().replaceFirst(request.getContextPath(), "");
        if (ignoreRequest(request, servletCtxStrippedRequestUri)) {
            // if there is not HstRequestContext object on the request, it will be ignored by other filters
        } else {
            req.setAttribute(HstRequestContext.class.getName(), new HstRequestContextImpl());
        }
        chain.doFilter(request, response);
    }

    protected boolean ignoreRequest(HttpServletRequest request, String servletCtxStrippedRequestUri) {
        if (request.getAttribute(HSTHttpAttributes.REQUEST_IGNORE_HSTPROCESSING_REQ_ATTRIBUTE) != null) {
            return true;
        }
        
        for (String prefix : ignorePathsList) {
            if (servletCtxStrippedRequestUri.startsWith(prefix)) {
                request.setAttribute(HSTHttpAttributes.REQUEST_IGNORE_HSTPROCESSING_REQ_ATTRIBUTE, "true");
                return true;
            }
        }
        for (String suffix : ignoreTypesList) {
            if (servletCtxStrippedRequestUri.endsWith(suffix)) {
                request.setAttribute(HSTHttpAttributes.REQUEST_IGNORE_HSTPROCESSING_REQ_ATTRIBUTE, "true");
                return true;
            }
        }
        return false;
    }
    
    protected void initIgnoreTypes(FilterConfig filterConfig) {
        String ignoreTypesString = filterConfig.getInitParameter(IGNORETYPES_FILTER_INIT_PARAM);
        ignoreTypesList = new ArrayList<String>();
        if (ignoreTypesString != null) {
            String[] items = ignoreTypesString.split(",");
            for (String item : items) {
                log.debug("filter configured with ignoretype .{}", item);
                ignoreTypesList.add("." + item.trim());
            }
        }
    }

    protected void initIgnorePaths(FilterConfig filterConfig) {
        String ignorePathsString = filterConfig.getInitParameter(IGNOREPATHS_FILTER_INIT_PARAM);
        ignorePathsList = new ArrayList<String>();
        if (ignorePathsString != null) {
            String[] items = ignorePathsString.split(",");
            for (String item : items) {
                log.debug("filter configured with ignorepath .{}", item);
                ignorePathsList.add(item.trim());
            }
        }
    }
}
