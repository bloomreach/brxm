package org.hippoecm.hst.core.container;

public interface Pipelines
{
    
    Pipeline getDefaultPipeline();
    
    Pipeline getPipeline(String name);
    
}
