package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface Pipeline
{
    
    void initialize() throws Exception;
    
    void invoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context) throws Exception;

    String getName();

}
