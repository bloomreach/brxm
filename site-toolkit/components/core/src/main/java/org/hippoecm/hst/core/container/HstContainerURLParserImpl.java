package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public class HstContainerURLParserImpl implements HstContainerURLParser {

    public HstContainerURL parseURL(ServletRequest servletRequest) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        
        HstContainerURLImpl url = new HstContainerURLImpl();
        
        url.setContextPath(request.getContextPath());
        url.setServletPath(request.getServletPath());
        url.setRenderPath(request.getPathInfo());
        
        Map<String, String []> paramMap = (Map<String, String []>) request.getParameterMap();
        url.setParameters(paramMap);
        
        return url;
    }

}
