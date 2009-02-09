package org.hippoecm.hst.core.container;

import java.util.List;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class CleanupValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException
    {
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
            }
        }
        
        // continue
        context.invokeNext(request);
    }
}
