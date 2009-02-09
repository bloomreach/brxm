package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface Pipeline
{
    
    void initialize() throws ContainerException;
    
    void beforeInvoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context) throws ContainerException;

    void invoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context) throws ContainerException;
    
    void afterInvoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context) throws ContainerException;

    void destroy() throws ContainerException;
    
}
