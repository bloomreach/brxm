package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ContextResolvingValve extends AbstractValve
{
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception
    {
        // TODO: resolve external url to content data source here and store it to HstRequest here.
        
        // continue
        context.invokeNext(request);
    }
}
