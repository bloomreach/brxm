/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
