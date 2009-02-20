/*
 *  Copyright 2008 Hippo.
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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstURL;

public class HstContainerURLImpl implements HstContainerURL {

    protected String contextPath;
    protected String servletPath;
    protected String renderPath;
    protected String type = HstURL.TYPE_RENDER;
    protected String actionWindow;
    protected String resourceWindow;
    protected Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public String getContextPath() {
        return this.contextPath;
    }
    
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }
    
    public String getServletPath() {
        return this.servletPath;
    }
    
    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }
    
    public String getRenderPath() {
        return this.renderPath;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return this.type;
    }
    
    public void setActionWindow(String actionWindow) {
        this.actionWindow = actionWindow;
    }
    
    public String getActionWindow() {
        return this.actionWindow;
    }
    
    public void setResourceWindow(String resourceWindow) {
        this.resourceWindow = resourceWindow;
    }
    
    public String getResourceWindow() {
        return this.resourceWindow;
    }
    
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public void setParameter(String name, String value) {
        this.parameterMap.put(name, new String [] { value });
    }

    public void setParameter(String name, String[] values) {
        this.parameterMap.put(name, values);
    }

    public void setParameters(Map<String, String[]> parameters) {
        this.parameterMap.putAll(parameters);
    }

}
