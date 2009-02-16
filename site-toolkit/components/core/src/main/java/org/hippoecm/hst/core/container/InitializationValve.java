package org.hippoecm.hst.core.container;

import java.util.List;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.request.HstRequestContext;

public class InitializationValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstRequestContext requestContext = getRequestContextComponent().create();
        ServletRequest servletRequest = context.getServletRequest();
        servletRequest.setAttribute(HstRequestContext.class.getName(), requestContext);
        
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
                resourceLifecycleManagement.setActive(true);
            }
        }
        
        // continue
        context.invokeNext();
    }
}
