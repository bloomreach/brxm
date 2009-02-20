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
package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLImpl;

public class HstURLProviderImpl implements HstURLProvider {
    
    protected HstContainerURL baseContainerURL;
    protected String parameterNamespace;
    protected String parameterNameComponentSeparator;
    
    protected String type;
    protected Map<String, String[]> parameterMap;
    
    public HstURLProviderImpl(HstContainerURL baseContainerURL, String parameterNamespace, String parameterNameComponentSeparator) {
        this.baseContainerURL = baseContainerURL;
        this.parameterNamespace = parameterNamespace;
        this.parameterNameComponentSeparator = (parameterNameComponentSeparator == null ? "" : parameterNameComponentSeparator);
    }

    public void clearParameters() {
        if (this.parameterMap != null) {
            this.parameterMap.clear();
        }
    }

    public void setParameters(Map<String, String[]> parameters) {
        if (this.parameterMap == null) {
            this.parameterMap = new HashMap<String, String[]>();
        }
        
        this.parameterMap.putAll(parameters);
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String toString() {
        Map<String, String[]> mergedParams = mergeParameters();
        
        String basePath = "";
        
        if (this.baseContainerURL != null) {
            HstContainerURLImpl containerURL = (HstContainerURLImpl) this.baseContainerURL;
            basePath = containerURL.getContextPath() + containerURL.getServletPath() + containerURL.getRenderPath();
        }
        
        StringBuilder sb = new StringBuilder(basePath);
        boolean firstDone = false;
        
        for (Map.Entry<String, String[]> entry : mergedParams.entrySet()) {
            String name = entry.getKey();
            
            for (String value : entry.getValue()) {
                sb.append(firstDone ? "&" : "?")
                .append(name)
                .append("=")
                .append(value);
                
                firstDone = true;
            }
        }
        
        return sb.toString();
    }

    protected Map<String, String []> mergeParameters() {
        Map<String, String[]> mergedParams = new HashMap<String, String[]>();
        
        if (this.baseContainerURL != null) {
            mergedParams.putAll(this.baseContainerURL.getParameterMap());
        }
        
        for (Map.Entry<String, String[]> entry : this.parameterMap.entrySet()) {
            String name = this.parameterNamespace + this.parameterNameComponentSeparator + entry.getKey();
            String [] values = entry.getValue();
            
            if (values == null || values.length == 0) {
                mergedParams.remove(name);
            } else {
                mergedParams.put(name, values);
            }
        }
        
        return mergedParams;
    }
}
