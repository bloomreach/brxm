package org.hippoecm.hst.core.container.pipeline;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface Pipeline
{
    
    void initialize() throws Exception;
    
    void invoke(HstRequestContext context) throws Exception;

    String getName();

}
