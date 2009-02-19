package org.hippoecm.hst.core.component;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestImpl extends HttpServletRequestWrapper implements HstRequest {
    
    protected String type = RENDER_TYPE;
    protected HstRequestContext requestContext;
    protected Map<String, Map<String, Object>> namespaceParametersMap = new HashMap<String, Map<String, Object>>();
    protected Map<String, Map<String, Object>> namespaceAttributesMap = new HashMap<String, Map<String, Object>>();
    protected HstComponentWindow componentWindow;
    protected String parameterNameComponentSeparator;
    
    public HstRequestImpl(HttpServletRequest servletRequest, HstRequestContext requestContext, HstComponentWindow componentWindow, String parameterNameComponentSeparator) {
        super(servletRequest);
        this.requestContext = requestContext;
        this.componentWindow = componentWindow;
        this.parameterNameComponentSeparator = (parameterNameComponentSeparator == null ? "" : parameterNameComponentSeparator);
    }
    
    public void setRequest(HttpServletRequest servletRequest) {
        super.setRequest(servletRequest);
    }

    public String getType() {
        return this.type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParameterMap() {
        String referenceNamespace = this.componentWindow.getReferenceNamespace();
        return getParameterMap(referenceNamespace);
    }
    
    public Map<String, Object> getParameterMap(String referencePath) {
        String namespace = getReferenceNamespacePath(referencePath);
        String prefix = getFullNamespacePrefix(namespace);
        int paramPrefixLen = prefix.length();
        Map<String, Object> parameterMap = this.namespaceParametersMap.get(prefix);
        
        if (parameterMap == null) {
            parameterMap = new HashMap<String, Object>();
            
            for (Enumeration paramNames = super.getParameterNames(); paramNames.hasMoreElements(); ) {
                String encodedParamName = (String) paramNames.nextElement();
                
                if (encodedParamName.startsWith(prefix)) {
                    String paramName = encodedParamName.substring(paramPrefixLen);
                    String [] paramValues = super.getParameterValues(encodedParamName);
                    parameterMap.put(paramName, paramValues.length > 1 ? paramValues : paramValues[0]);
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
    public Object getAttribute(String name) {
        if (name.startsWith("javax.servlet.")) {
            return super.getAttribute(name);
        } else {
            return getAttributeMap().get(name);
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name.startsWith("javax.servlet.")) {
            super.setAttribute(name, value);
        } else {
            getAttributeMap().put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (name.startsWith("javax.servlet.")) {
            super.removeAttribute(name);
        } else {
            getAttributeMap().remove(name);
        }
    }
    
    public HstRequestContext getRequestContext() {
        return (HstRequestContext) super.getAttribute(HstRequestContext.class.getName());
    }

    public HstComponentWindow getComponentWindow() {
        return this.componentWindow;
    }
    
    public String getResourceID() {
        return null;
    }

    protected String getReferenceNamespacePath(String referencePath) {
        // TODO: find the reference name of the current request context by both absolute namespace and relative namespace.
        return referencePath;
    }
    
    protected String getFullNamespacePrefix(String referenceNamespace) {
        String prefix = null;
        
        String contextNamespace = this.requestContext.getContextNamespace();
        
        if (contextNamespace != null && !"".equals(contextNamespace)) {
            prefix = contextNamespace + this.parameterNameComponentSeparator + referenceNamespace + this.parameterNameComponentSeparator;
        } else {
            prefix = referenceNamespace + this.parameterNameComponentSeparator;
        }
        
        return prefix;
    }
    
}
