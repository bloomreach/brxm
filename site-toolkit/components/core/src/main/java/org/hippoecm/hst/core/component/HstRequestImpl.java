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

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.collections.collection.CompositeCollection;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestImpl extends HttpServletRequestWrapper implements HstRequest {
    
    protected String type = RENDER_TYPE;
    protected HstRequestContext requestContext;
    protected Map<String, Map<String, Object>> namespaceParametersMap = new HashMap<String, Map<String, Object>>();
    protected Map<String, Map<String, Object>> namespaceAttributesMap = new HashMap<String, Map<String, Object>>();
    protected HstComponentWindow componentWindow;
    protected String parameterNameComponentSeparator;
    
    public HstRequestImpl(HttpServletRequest servletRequest, HstRequestContext requestContext, HstComponentWindow componentWindow) {
        super(servletRequest);
        this.requestContext = requestContext;
        this.componentWindow = componentWindow;
        this.parameterNameComponentSeparator = requestContext.getURLFactory().getUrlProvider().getParameterNameComponentSeparator();
        
        HstContainerURL baseURL = this.requestContext.getBaseURL();
        
        if (baseURL.getActionWindowReferenceNamespace() != null) {
            this.type = ACTION_TYPE;
        } else if (baseURL.getResourceWindowReferenceNamespace() != null) {
            this.type = RESOURCE_TYPE;
        }
    }
    
    public void setRequest(HttpServletRequest servletRequest) {
        super.setRequest(servletRequest);
    }

    public String getType() {
        return this.type;
    }
    
    public Map<String, Object> getParameterMap() {
        String referenceNamespace = this.componentWindow.getReferenceNamespace();
        return getParameterMap(referenceNamespace);
    }
    
    public Map<String, Object> getParameterMap(String referencePath) {
        Map<String, Object> parameterMap = null;
        
        String namespace = getReferenceNamespacePath(referencePath);
        String prefix = getFullNamespacePrefix(namespace);
        int paramPrefixLen = prefix.length();
        parameterMap = this.namespaceParametersMap.get(prefix);
        
        if (parameterMap == null) {
            parameterMap = new HashMap<String, Object>();

            if (ACTION_TYPE.equals(getType())) {
                Map<String, String []> actionParams = this.requestContext.getBaseURL().getActionParameterMap();
                
                if (actionParams != null) {
                    for (Map.Entry<String, String []> entry : actionParams.entrySet()) {
                        String paramName = entry.getKey();
                        String [] paramValues = entry.getValue();
                        parameterMap.put(paramName, paramValues.length > 1 ? paramValues : paramValues[0]);
                    }
                }
                
                for (Enumeration paramNames = super.getParameterNames(); paramNames.hasMoreElements(); ) {
                    String paramName = (String) paramNames.nextElement();
                    String [] paramValues = super.getParameterValues(paramName);
                    parameterMap.put(paramName, paramValues.length > 1 ? paramValues : paramValues[0]);
                }
            } else {
                for (Enumeration paramNames = super.getParameterNames(); paramNames.hasMoreElements(); ) {
                    String encodedParamName = (String) paramNames.nextElement();
                    
                    if (encodedParamName.startsWith(prefix)) {
                        String paramName = encodedParamName.substring(paramPrefixLen);
                        String [] paramValues = super.getParameterValues(encodedParamName);
                        parameterMap.put(paramName, paramValues.length > 1 ? paramValues : paramValues[0]);
                    }
                }
            }
            
            this.namespaceParametersMap.put(prefix, parameterMap);
        }
        
        return parameterMap;
    }
    
    public Map<String, Object> getAttributeMap() {
        String referenceNamespace = this.componentWindow.getReferenceNamespace();
        return getAttributeMap(referenceNamespace);
    }
    
    public Map<String, Object> getAttributeMap(String referencePath) {
        String namespace = getReferenceNamespacePath(referencePath);
        String prefix = getFullNamespacePrefix(namespace);
        int prefixLen = prefix.length();
        Map<String, Object> attributesMap = this.namespaceAttributesMap.get(prefix);
        
        if (attributesMap == null) {
            attributesMap = new HashMap<String, Object>();
            
            for (Enumeration attributeNames = super.getAttributeNames(); attributeNames.hasMoreElements(); ) {
                String encodedAttributeName = (String) attributeNames.nextElement();
                
                if (encodedAttributeName.startsWith(prefix)) {
                    String attributeName = encodedAttributeName.substring(prefixLen);
                    Object attributeValue = super.getAttribute(encodedAttributeName);
                    attributesMap.put(attributeName, attributeValue);
                }
            }
            
            this.namespaceAttributesMap.put(prefix, attributesMap);
        }
        
        return attributesMap;
    }

    @Override
    public String getParameter(String name) {
        Object value = getParameterMap().get(name);

        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return (((String[]) value)[0]);
        } else if (value instanceof String) {
            return ((String) value);
        } else {
            return (value.toString());
        }
    }

    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(this.getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        Object value = getParameterMap().get(name);
        
        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return ((String[]) value);
        } else if (value instanceof String) {
            return new String [] { (String) value };
        } else {
            return new String [] { value.toString() };
        }
    }

    @Override
    public Enumeration getAttributeNames() {
        List servletRequestAttrs = EnumerationUtils.toList(super.getAttributeNames());
        Set localRequestAttrs = this.getAttributeMap().keySet();
        Collection composite = new CompositeCollection(new Collection [] { servletRequestAttrs, localRequestAttrs });
        return Collections.enumeration(composite);
    }
    
    @Override
    public Object getAttribute(String name) {
        Object value = null;
        
        if (name.startsWith("javax.")) {
            value = super.getAttribute(name);
        } else {
            value = getAttributeMap().get(name);
            
            if (value == null) {
                value = super.getAttribute(name);
            }
        }
        
        return value;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name.startsWith("javax.")) {
            super.setAttribute(name, value);
        } else {
            getAttributeMap().put(name, value);
            // Set attribute into the servlet request as well
            // because some containers set their specific attributes for later use.
            super.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (name.startsWith("javax.")) {
            super.removeAttribute(name);
        } else {
            Object value = getAttributeMap().remove(name);
            
            // Remove attribute from the servlet request
            // if no attribute was removed from the this local request attributes.
            if (value == null) {
                super.removeAttribute(name);
            }
        }
    }
    
    public HstRequestContext getRequestContext() {
        return (HstRequestContext) super.getAttribute(HstRequestContext.class.getName());
    }

    public HstComponentWindow getComponentWindow() {
        return this.componentWindow;
    }
    
    public String getResourceID() {
        return this.requestContext.getBaseURL().getResourceId();
    }

    protected String getReferenceNamespacePath(String referencePath) {
        // TODO: find the reference name of the current request context by both absolute namespace and relative namespace.
        return referencePath;
    }
    
    protected String getFullNamespacePrefix(String referenceNamespace) {
        String prefix = referenceNamespace + this.parameterNameComponentSeparator;
        return prefix;
    }

}
