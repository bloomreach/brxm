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
package org.hippoecm.hst.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.service.ServiceBeanAccessProvider;

/**
 * @deprecated since 2.28.05 (CMS 7.9.1). Do not use any more. No replacement
 */
@Deprecated
public class NamespacedBeanMethodInvoker implements Invoker, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ServiceBeanAccessProvider provider;
    private String primaryJcrType;
    private String namespacePrefix;
    
    public NamespacedBeanMethodInvoker(final ServiceBeanAccessProvider provider, String primaryJcrType) {
        this.provider = provider;
        this.primaryJcrType = primaryJcrType;
        int offset = primaryJcrType.indexOf(':');
        this.namespacePrefix = (offset != -1 ? primaryJcrType.substring(0, offset) : primaryJcrType);
    }
    
    public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
        String overridenPrimaryJcrType = getOverridenPrimaryJcrType(method);
        String overridenNamespacePrefix = null;
        
        if (this.primaryJcrType.equals(overridenPrimaryJcrType)) {
            overridenNamespacePrefix = this.namespacePrefix;
        } else {
            int offset = overridenPrimaryJcrType.indexOf(';');
            overridenNamespacePrefix = (offset != -1 ? overridenPrimaryJcrType.substring(0, offset) : overridenPrimaryJcrType);
        }
        
        String methodName = method.getName();
        Class [] paramTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        
        if (methodName.startsWith("get") && paramTypes.length == 0) {
            String propName = getCamelString(methodName.substring(3));
            return provider.getProperty(overridenNamespacePrefix, propName, returnType, method);
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            String propName = getCamelString(methodName.substring(2));
            return provider.getProperty(overridenNamespacePrefix, propName, returnType, method);
        } else if (methodName.startsWith("set") && paramTypes.length == 1) {
            String propName = getCamelString(methodName.substring(3));
            return provider.setProperty(overridenNamespacePrefix, propName, args[0], returnType, method);
        } else {
            return provider.invoke(overridenNamespacePrefix, methodName, args, returnType, method);
        }
    }
    
    private String getOverridenPrimaryJcrType(Method method) {
        String overridenPrimaryJcrType = this.primaryJcrType;
        
        if (method.isAnnotationPresent(Node.class)) {
            overridenPrimaryJcrType = method.getAnnotation(Node.class).jcrType();
        } else if (method.getDeclaringClass().isAnnotationPresent(Node.class)) {
            overridenPrimaryJcrType = method.getDeclaringClass().getAnnotation(Node.class).jcrType();
        }
        
        return overridenPrimaryJcrType;
    }
    
    private static String getCamelString(String s) {
        char firstChar = s.charAt(0);
        
        if (Character.isUpperCase(firstChar)) {
            StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(0, Character.toLowerCase(firstChar));
            s = sb.toString();
        }
        
        return s;
    }

}
