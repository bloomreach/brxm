package org.hippoecm.hst.core.container;

import java.util.Map;

public class HstSitePipelines implements Pipelines
{
    
    protected String defaultPipelineName;
    protected Map<String, Pipeline> pipelines;
    
    public HstSitePipelines()
    {
    }
    
    public void setDefaultPipelineName(String defaultPipelineName) {
        this.defaultPipelineName = defaultPipelineName;
    }
    
    public String getDefaultPipelineName() {
        return this.defaultPipelineName;
    }
    
    public Pipeline getDefaultPipeline()
    {
        return this.pipelines.get(this.defaultPipelineName);
    }

    public void setPipelines(Map<String, Pipeline> pipelines) {
        this.pipelines = pipelines;
    }
    
    public Map<String, Pipeline> getPipelines() {
        return this.pipelines;
    }
    
    public Pipeline getPipeline(String name)
    {
        return this.pipelines.get(name);
    }
    
}
