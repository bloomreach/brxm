package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


public interface ValveContext
{
    public void invokeNext() throws ContainerException;
    
    public ServletConfig getServletConfig();
    
    public ServletRequest getServletRequest();

    public ServletResponse getServletResponse();
    
    public void setRootComponentWindow(HstComponentWindow rootComponentWindow);
    
    public HstComponentWindow getRootComponentWindow();
    
}
