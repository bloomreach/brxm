package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface Pipeline
{
    
    void initialize() throws ContainerException;
    
    void beforeInvoke(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invoke(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void afterInvoke(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void destroy() throws ContainerException;
    
}
