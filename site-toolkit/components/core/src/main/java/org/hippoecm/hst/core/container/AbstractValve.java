package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public abstract class AbstractValve implements Valve
{

    public abstract void invoke(HstRequestContext request, ValveContext context) throws Exception;

    public void initialize() throws Exception
    {
    }
    
    protected boolean isActionRequest() {
        return false;
    }
    
    protected boolean isResourceRequest() {
        return false;
    }
    
}
