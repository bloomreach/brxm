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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HstContainerURLImpl
 * 
 * @version $Id$
 */
public class HstContainerURLImpl implements HstContainerURL, Cloneable {

    protected String characterEncoding;
    protected String contextPath;
    protected String hostName;
    protected String requestPath;
    protected String resolvedMountPath;
    protected String pathInfo;
    protected int portNumber;
    protected String actionWindowReferenceNamespace;
    protected String resourceWindowReferenceNamespace;
    protected String componentRenderingReferenceNamespace;
    protected String resourceId;
    protected Map<String, String[]> parameterMap;
    protected Map<String, String[]> actionParameterMap;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getResolvedMountPath() {
        return resolvedMountPath;
    }

    public void setResolvedMountPath(String resolvedMountPath) {
        this.resolvedMountPath = resolvedMountPath;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public String getContextPath() {
        return this.contextPath;
    }
  
    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }
    
    public String getPathInfo() {
        return this.pathInfo;
    }
    
    public void setActionWindowReferenceNamespace(String actionWindowReferenceNamespace) {
        this.actionWindowReferenceNamespace = actionWindowReferenceNamespace;
    }
    
    public String getActionWindowReferenceNamespace() {
        return this.actionWindowReferenceNamespace;
    }
    
    public void setResourceWindowReferenceNamespace(String resourceWindowReferenceNamespace) {
        this.resourceWindowReferenceNamespace = resourceWindowReferenceNamespace;
    }

    public String getResourceWindowReferenceNamespace() {
        return this.resourceWindowReferenceNamespace;
    }

    @Override
    public String getComponentRenderingWindowReferenceNamespace() {
        return componentRenderingReferenceNamespace;
    }

    @Override
    public void setComponentRenderingWindowReferenceNamespace(final String referenceNamespace) {
        this.componentRenderingReferenceNamespace = referenceNamespace;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getResourceId() {
        return this.resourceId;
    }
    
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public void setParameter(String name, String value) {
        setParameter(name, value != null ? new String [] { value } : (String []) null);
    }

    public void setParameter(String name, String[] values) {
        if (this.parameterMap == null) {
            // keep insertion ordered map to maintain the order of the querystring when re-constructing it from a map
            this.parameterMap = new LinkedHashMap<String, String[]>();
        }

        if (values == null || values.length == 0) {
            this.parameterMap.remove(name);
        } else {
            this.parameterMap.put(name, values);
        }
    }
    
    public void setParameters(Map<String, String[]> parameters) {
        if (parameters == null) {
            if (this.parameterMap != null) {
                this.parameterMap.clear();
            }
        } else {
            String [] paramValues = null;
            String [] clonedParamValues = null;
            
            for (Map.Entry<String, String []> entry : parameters.entrySet()) {
                paramValues = entry.getValue();
                
                if (paramValues == null) {
                    clonedParamValues = null;
                } else {
                    clonedParamValues = new String[paramValues.length];
                    System.arraycopy(paramValues, 0, clonedParamValues, 0, paramValues.length);
                }
                
                setParameter(entry.getKey(), clonedParamValues);
            }
        }
    }
    
    public Map<String, String []> getActionParameterMap() {
        return this.actionParameterMap;
    }
    
    public void setActionParameter(String name, String value) {
        setActionParameter(name, value != null ? new String [] { value } : (String []) null);
    }

    public void setActionParameter(String name, String[] values) {
        if (this.actionParameterMap == null) {
            this.actionParameterMap = new HashMap<String, String[]>();
        }

        if (values == null) {
            this.actionParameterMap.remove(name);
        } else {
            this.actionParameterMap.put(name, values);
        }
    }

    public void setActionParameters(Map<String, String[]> parameters) {
        if (parameters == null) {
            if (this.actionParameterMap != null) {
                this.actionParameterMap.clear();
            }
        } else {
            String [] paramValues = null;
            String [] clonedParamValues = null;
            
            for (Map.Entry<String, String []> entry : parameters.entrySet()) {
                paramValues = entry.getValue();
                
                if (paramValues == null) {
                    clonedParamValues = null;
                } else {
                    clonedParamValues = new String[paramValues.length];
                    System.arraycopy(paramValues, 0, clonedParamValues, 0, paramValues.length);
                }
                
                setActionParameter(entry.getKey(), clonedParamValues);
            }
        }
    }
    
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }
    
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        HstContainerURLImpl cloned = (HstContainerURLImpl) super.clone();
        
        cloned.characterEncoding = this.characterEncoding;
        cloned.contextPath = this.contextPath;
        cloned.hostName = this.hostName;
        cloned.requestPath = this.requestPath;
        cloned.resolvedMountPath = this.resolvedMountPath;
        cloned.portNumber = this.portNumber;
        cloned.pathInfo = this.pathInfo;
        cloned.actionWindowReferenceNamespace = this.actionWindowReferenceNamespace;
        cloned.resourceWindowReferenceNamespace = this.resourceWindowReferenceNamespace;
        cloned.componentRenderingReferenceNamespace = this.componentRenderingReferenceNamespace;
        cloned.resourceId = this.resourceId;
        
        cloned.actionParameterMap = null;
        
        if (this.actionParameterMap != null) {
            cloned.setActionParameters(this.actionParameterMap);
        }
        
        cloned.parameterMap = null;
        
        if (this.parameterMap != null) {
            cloned.setParameters(this.parameterMap);
        }
        
        return cloned;
    }

}
