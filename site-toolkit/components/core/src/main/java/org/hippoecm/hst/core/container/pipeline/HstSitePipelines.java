package org.hippoecm.hst.core.container.pipeline;

import java.util.HashMap;
import java.util.Map;

public class HstSitePipelines implements Pipelines
{
    
    protected Map<String, Pipeline> pipelineMap;
    protected String defaultPipelineName;
    
    public HstSitePipelines(Pipeline [] pipelines, String defaultPipelineName)
    {
        this.defaultPipelineName = defaultPipelineName;
        this.pipelineMap = new HashMap<String, Pipeline>();
        
        for (Pipeline pipeline : pipelines)
        {
            this.pipelineMap.put(pipeline.getName(), pipeline);
        }
    }
    
    public Pipeline getDefaultPipeline()
    {
        return this.pipelineMap.get(this.defaultPipelineName);
    }

    public Pipeline getPipeline(String name)
    {
        return this.pipelineMap.get(name);
    }
    
}
