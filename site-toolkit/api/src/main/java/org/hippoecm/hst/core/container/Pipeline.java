package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface Pipeline
{
    
    void initialize() throws ContainerException;
    
    void beforeInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void afterInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void destroy() throws ContainerException;
    
}
