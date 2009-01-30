package org.hippoecm.hst.core.container.pipeline.valve;

import org.hippoecm.hst.core.container.pipeline.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception
    {
        
        // continue
        context.invokeNext(request);
    }
}
