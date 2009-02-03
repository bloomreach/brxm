package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve
{
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception
    {
        
        // continue
        context.invokeNext(request);
    }
}
