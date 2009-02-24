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
    
    protected String parameterNameComponentSeparator = "_";
    
    public HstURLProviderImpl() {
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
    }

    public String createURLString(HstContainerURL baseContainerURL, HstURL hstUrl) {
        String characterEncoding = hstUrl.getCharacterEncoding();
        String parameterNamespace = hstUrl.getParameterNamespace();
        Map<String, String []> parameters = hstUrl.getParameterMap();
        
        Map<String, String[]> mergedParams = mergeParameters(baseContainerURL, parameterNamespace, parameters);
        
        HstContainerURLImpl containerURL = (HstContainerURLImpl) baseContainerURL;
        String baseUrlPath = containerURL.getContextPath() + containerURL.getServletPath() + containerURL.getRenderPath();
        
        StringBuilder sb = new StringBuilder(baseUrlPath);
        boolean firstParamDone = (sb.indexOf("?") >= 0);
        
        for (Map.Entry<String, String[]> entry : mergedParams.entrySet()) {
            String name = entry.getKey();
            
            for (String value : entry.getValue()) {
                String encodedValue = value;
                try {
                    encodedValue = URLEncoder.encode(value, characterEncoding);
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
    
    protected Map<String, String []> mergeParameters(HstContainerURL baseContainerURL, String parameterNamespace, Map<String, String []> parameterMap) {
        Map<String, String[]> mergedParams = new HashMap<String, String[]>();
        
        if (baseContainerURL != null) {
            mergedParams.putAll(baseContainerURL.getParameterMap());
        }
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = parameterNamespace + this.parameterNameComponentSeparator + entry.getKey();
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
