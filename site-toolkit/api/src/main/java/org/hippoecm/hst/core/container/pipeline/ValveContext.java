package org.hippoecm.hst.core.container.pipeline;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface ValveContext
{
    public void invokeNext(HstRequestContext request) throws Exception;
}
