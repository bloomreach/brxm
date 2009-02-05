package org.hippoecm.hst.core.component;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestImpl extends HttpServletRequestWrapper implements HstRequest {
    
    protected HstRequestContext requestContext;
    protected HstComponentConfiguration componentConfiguration;
    protected Map<String, Map<String, Object>> namespaceParametersMap = new HashMap<String, Map<String, Object>>();
    protected Map<String, Map<String, Object>> namespaceAttributesMap = new HashMap<String, Map<String, Object>>();
    
    public HstRequestImpl(HttpServletRequest servletRequest, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) {
        super(servletRequest);
        this.requestContext = requestContext;
        this.componentConfiguration = componentConfiguration;
    }

    public Map<String, Object> getParameterMap() {
        String namespace = this.componentConfiguration.getNamespace();
        return getParameterMap(namespace);
    }
    
    public Map<String, Object> getParameterMap(String namespace) {
        String reference = getReferenceByNamespace(namespace);
        int referenceLen = reference.length();
        Map<String, Object> parameterMap = this.namespaceParametersMap.get(namespace);
        
        if (parameterMap == null) {
            parameterMap = new HashMap<String, Object>();
            
            for (Enumeration paramNames = super.getParameterNames(); paramNames.hasMoreElements(); ) {
                String encodedParamName = (String) paramNames.nextElement();
                
                if (encodedParamName.startsWith(reference)) {
                    String paramName = encodedParamName.substring(referenceLen);
                    String [] paramValues = super.getParameterValues(encodedParamName);
                    parameterMap.put(paramName, paramValues.length > 1 ? paramValues : paramValues[0]);
                }
            }
            
            this.namespaceParametersMap.put(namespace, parameterMap);
        }
        
        return parameterMap;
    }
    
    public Map<String, Object> getAttributeMap() {
        String namespace = this.componentConfiguration.getNamespace();
        return getAttributeMap(namespace);
    }
    
    public Map<String, Object> getAttributeMap(String namespace) {
        String reference = getReferenceByNamespace(namespace);
        int referenceLen = reference.length();
        Map<String, Object> attributesMap = this.namespaceAttributesMap.get(namespace);
        
        if (attributesMap == null) {
            attributesMap = new HashMap<String, Object>();
            
            for (Enumeration attributeNames = super.getAttributeNames(); attributeNames.hasMoreElements(); ) {
                String encodedAttributeName = (String) attributeNames.nextElement();
                
                if (encodedAttributeName.startsWith(reference)) {
                    String attributeName = encodedAttributeName.substring(referenceLen);
                    Object attributeValue = super.getAttribute(encodedAttributeName);
                    attributesMap.put(attributeName, attributeValue);
                }
            }
            
            this.namespaceAttributesMap.put(namespace, attributesMap);
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
    public Object getAttribute(String name) {
        return getAttributeMap().get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        getAttributeMap().put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getAttributeMap().remove(name);
    }
    
    protected String getReferenceByNamespace(String namespace) {
        // TODO: find the reference name of the current request context by both absolute namespace and relative namespace.
        return namespace;
    }

}
