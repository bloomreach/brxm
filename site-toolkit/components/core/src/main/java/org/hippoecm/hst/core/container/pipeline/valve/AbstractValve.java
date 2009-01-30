package org.hippoecm.hst.core.container.pipeline.valve;

import org.hippoecm.hst.core.container.pipeline.Valve;
import org.hippoecm.hst.core.container.pipeline.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public abstract class AbstractValve implements Valve
{
    public abstract void invoke(HstRequestContext request, ValveContext context) throws Exception;

    public void initialize() throws Exception
    {
    }
}
