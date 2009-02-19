package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLImpl;

public class HstURLProviderImpl implements HstURLProvider {
    
    protected HstContainerURL baseContainerURL;
    protected String parameterNamespace;
    protected String type;
    protected Map<String, String[]> parameterMap;
    
    public HstURLProviderImpl(HstContainerURL baseContainerURL, String parameterNamespace) {
        this.baseContainerURL = baseContainerURL;
        this.parameterNamespace = parameterNamespace;
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
            String name = this.parameterNamespace + entry.getKey();
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
