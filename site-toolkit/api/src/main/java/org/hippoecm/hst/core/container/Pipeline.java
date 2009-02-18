package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface Pipeline
{
    
    void initialize() throws ContainerException;
    
    void beforeInvoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void afterInvoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void destroy() throws ContainerException;
    
}
