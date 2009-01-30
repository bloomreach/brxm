package org.hippoecm.hst.core.container.pipeline;

public interface Pipelines
{
    
    Pipeline getDefaultPipeline();
    
    Pipeline getPipeline(String name);
    
}
