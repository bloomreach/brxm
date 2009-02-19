package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        if (isActionRequest()) {
            
            HstComponentWindow window = findActionWindow(context.getRootComponentWindow());
            
            if (window != null) {
                ServletRequest servletRequest = context.getServletRequest();
                ServletResponse servletResponse = context.getServletResponse();
                HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());

                HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, getUrlFactory().getParameterNameComponentSeparator());
                HstResponseState responseState = new HstResponseState((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
                HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window, responseState);
                ((HstComponentWindowImpl) window).setResponseState(responseState);

                getComponentInvoker().invokeAction(context.getServletContext(), request, response);
            } else {
                throw new ContainerException("No action window found.");
            }
        }
        
        // continue
        context.invokeNext();
    }
    
    protected void sendRedirectNavigation(HstRequestContext request) {
        
    }
    
    protected HstComponentWindow findActionWindow(HstComponentWindow rootWindow) {
        HstComponentWindow actionWindow = null;
        
        return actionWindow;
    }
}
