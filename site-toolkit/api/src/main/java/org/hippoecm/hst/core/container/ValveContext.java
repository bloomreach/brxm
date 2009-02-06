package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface ValveContext
{
    public void invokeNext(HstRequestContext request) throws Exception;
    
    public ServletRequest getServletRequest();

    public ServletResponse getServletResponse();
    
}
