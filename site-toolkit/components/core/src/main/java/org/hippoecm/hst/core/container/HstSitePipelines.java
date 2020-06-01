/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.util.Map;

/**
 * HstSitePipelines
 * 
 * @version $Id$
 */
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
