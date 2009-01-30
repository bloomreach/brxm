package org.hippoecm.hst.core.container.pipeline;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface Valve
{
    public void invoke(HstRequestContext request, ValveContext context) throws Exception;

    /**
     * Initialize the valve before using in a pipeline.
     */
    public void initialize() throws Exception;

}