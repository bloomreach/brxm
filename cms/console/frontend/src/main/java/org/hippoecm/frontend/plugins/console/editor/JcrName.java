package org.hippoecm.frontend.plugins.console.editor;

import org.apache.commons.lang.StringUtils;

/**
 * Wrapper around a JCR name.
 */
public class JcrName {

    private static final String NAMESPACE_NAME_SEPARATOR = ":";

    private final String jcrPropName;
    
    JcrName(String jcrPropName) {
        this.jcrPropName = jcrPropName;        
    }

    public boolean hasNamespace() {
        return jcrPropName != null && jcrPropName.contains(":");
    }
    
    /**
     * @return the namespace part of the JCR property name, or null if the property does not have a namespace.
     */
    public String getNamespace() {
        if (hasNamespace()) {
            return StringUtils.substringBefore(jcrPropName, NAMESPACE_NAME_SEPARATOR);
        }
        return null;
    }

    /**
     * @return the local name of the property (i.e. the part without the namespace).
     */
    public String getName() {
        if (hasNamespace()) {
            return StringUtils.substringAfter(jcrPropName, NAMESPACE_NAME_SEPARATOR);
        }
        return jcrPropName;
    }

}
