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

public class HstContainerURLImpl implements HstContainerURL, Cloneable {

    protected String characterEncoding;
    protected String contextPath;
    protected String servletPath;
    protected String pathInfo;
    protected String actionWindowReferenceNamespace;
    protected String resourceWindowReferenceNamespace;
    protected String resourceId;
    protected Map<String, String[]> parameterMap;
    protected Map<String, String[]> actionParameterMap;
    protected boolean navigational;
    
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
            this.parameterMap = new HashMap<String, String[]>();
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
            for (Map.Entry<String, String []> entry : parameters.entrySet()) {
                setParameter(entry.getKey(), entry.getValue());
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
        for (Map.Entry<String, String []> entry : parameters.entrySet()) {
            setActionParameter(entry.getKey(), entry.getValue());
        }
    }
    
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }
    
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    
    public boolean isNavigational() {
        return navigational;
    }

    public void setNavigational(boolean navigational) {
        this.navigational = navigational;
    }

    @Override
    public Object clone() {
        HstContainerURLImpl cloned = new HstContainerURLImpl();
        
        cloned.characterEncoding = this.characterEncoding;
        cloned.contextPath = this.contextPath;
        cloned.servletPath = this.servletPath;
        cloned.pathInfo = this.pathInfo;
        cloned.actionWindowReferenceNamespace = this.actionWindowReferenceNamespace;
        cloned.resourceWindowReferenceNamespace = this.resourceWindowReferenceNamespace;
        cloned.resourceId = this.resourceId;
        
        if (this.parameterMap != null) {
            cloned.setParameters(this.parameterMap);
        }
        
        return cloned;
    }
 
}
