package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class HstURLValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        ServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
        
        HstContainerURL baseURL = getContainerURLParser().parseURL(context.getServletRequest());
        
        ((HstRequestContextImpl) requestContext).setBaseURL(baseURL);
        ((HstRequestContextImpl) requestContext).setURLFactory(getUrlFactory());
        
        // continue
        context.invokeNext();
    }

}
