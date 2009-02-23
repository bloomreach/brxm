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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLImpl;

public class HstURLProviderImpl implements HstURLProvider {
    
    protected String characterEncoding;
    protected HstContainerURL baseContainerURL;
    protected String parameterNamespace;
    protected String parameterNameComponentSeparator;
    
    protected String type;
    protected Map<String, String[]> parameterMap;
    
    protected String basePath;
    protected String baseContext;
    
    public HstURLProviderImpl(String characterEncoding, HstContainerURL baseContainerURL, String parameterNamespace, String parameterNameComponentSeparator) {
        this.characterEncoding = (characterEncoding != null ? characterEncoding : "UTF-8");
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
        
        String baseUrlPath = "";
        HstContainerURLImpl containerURL = (HstContainerURLImpl) this.baseContainerURL;
        
        if (this.basePath != null) {
            if (this.baseContext != null) {
                baseUrlPath = this.baseContext + containerURL.getServletPath() + this.basePath;
            } else {
                baseUrlPath = containerURL.getContextPath() + containerURL.getServletPath() + this.basePath;
            }
        } else if (this.baseContainerURL != null) {
            baseUrlPath = containerURL.getContextPath() + containerURL.getServletPath() + containerURL.getRenderPath();
        } 
        
        StringBuilder sb = new StringBuilder(baseUrlPath);
        boolean firstParamDone = (sb.indexOf("?") >= 0);
        
        for (Map.Entry<String, String[]> entry : mergedParams.entrySet()) {
            String name = entry.getKey();
            
            for (String value : entry.getValue()) {
                String encodedValue = value;
                try {
                    encodedValue = URLEncoder.encode(value, this.characterEncoding);
                } catch (Exception e) {
                }
                
                sb.append(firstParamDone ? "&" : "?")
                .append(name)
                .append("=")
                .append(encodedValue);
                
                firstParamDone = true;
            }
        }
        
        return sb.toString();
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public void setBaseContext(String baseContext) {
        this.baseContext = baseContext;
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
