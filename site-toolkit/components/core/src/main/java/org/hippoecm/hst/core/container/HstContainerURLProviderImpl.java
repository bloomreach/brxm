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

import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURL;

public class HstContainerURLProviderImpl implements HstContainerURLProvider {
    
    protected String parameterNameComponentSeparator = "_";
    
    public HstContainerURLProviderImpl() {
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
    }

    public HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        HstContainerURLImpl url = new HstContainerURLImpl();
        
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        url.setCharacterEncoding(characterEncoding);
        url.setContextPath(request.getContextPath());
        url.setServletPath(request.getServletPath());
        url.setPathInfo(request.getPathInfo());
        
        Map<String, String []> paramMap = (Map<String, String []>) request.getParameterMap();
        url.setParameters(paramMap);
        
        return url;
    }

    public HstContainerURL createURL(HstContainerURL baseContainerURL, HstURL hstUrl) {
        HstContainerURLImpl containerURL = (HstContainerURLImpl) ((HstContainerURLImpl) baseContainerURL).clone();
        mergeParameters(containerURL, hstUrl);
        return containerURL;
    }
    
    public String toURLString(HstContainerURL containerURL) {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(containerURL.getContextPath());
        url.append(containerURL.getServletPath());
        url.append(containerURL.getPathInfo());
        
        boolean firstParamDone = (url.indexOf("?") >= 0);
        
        Map<String, String []> parameters = containerURL.getParameterMap();
        
        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();
                
                for (String value : entry.getValue()) {
                    String encodedValue = value;
                    try {
                        encodedValue = URLEncoder.encode(value, characterEncoding);
                    } catch (Exception e) {
                    }
                    
                    url.append(firstParamDone ? "&" : "?")
                    .append(name)
                    .append("=")
                    .append(encodedValue);
                    
                    firstParamDone = true;
                }
            }
        }
        
        return url.toString();
    }
    
    protected void mergeParameters(HstContainerURL containerURL, HstURL hstUrl) {
        String parameterNamespace = hstUrl.getParameterNamespace();
        Map<String, String []> parameterMap = hstUrl.getParameterMap();
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = parameterNamespace + this.parameterNameComponentSeparator + entry.getKey();
            String [] values = entry.getValue();
            
            if (values == null || values.length == 0) {
                containerURL.setParameter(name, (String) null);
            } else {
                containerURL.setParameter(name, values);
            }
        }
    }
}
