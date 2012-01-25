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
package org.hippoecm.hst.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.proxy.Invoker;

/**
 * DynaBeanMethodInvoker
 * 
 * @version $Id$
 */
public class DynaBeanMethodInvoker implements Invoker, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private DynaBean dynaBean;
    
    public DynaBeanMethodInvoker(final DynaBean dynaBean) {
        this.dynaBean = dynaBean;
    }
    
    public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
        String methodName = method.getName();

        Class<?> [] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        
        if (methodName.startsWith("get")) {
            String propName = getCamelString(methodName.substring(3));

            if (paramTypes.length == 0) {
                return this.dynaBean.get(propName);
            } else if (paramTypes.length == 1) {
                if (paramTypes[0] == int.class || paramTypes[0] == Integer.class) {
                    return this.dynaBean.get(propName, ((Integer) args[0]).intValue());
                } else if (paramTypes[0] == String.class) {
                    return this.dynaBean.get(propName, (String) args[0]);
                } else {
                    throw new UnsupportedOperationException("No getter for " + propName + " with " + paramTypes[0] + " type.");
                }
            } else {
                throw new UnsupportedOperationException("No getter for " + propName + " with " + paramTypes.length + " parameters.");
            }
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            String propName = getCamelString(methodName.substring(2));
            return this.dynaBean.get(propName);
        } else if (methodName.startsWith("set")) {
            String propName = getCamelString(methodName.substring(3));
            if (paramTypes.length == 1) {
                this.dynaBean.set(propName, args[0]);
            } else if (paramTypes.length == 2) {
                if (paramTypes[0] == int.class || paramTypes[0] == Integer.class) {
                    this.dynaBean.set(propName, ((Integer) args[0]).intValue(), args[1]);
                } else if (paramTypes[0] == String.class) {
                    this.dynaBean.set(propName, (String) args[0], args[1]);
                } else {
                    throw new UnsupportedOperationException("No setter for " + propName + " with " + paramTypes[1] + " type.");
                }
                
            } else {
                throw new UnsupportedOperationException("No setter for " + propName + " with " + paramTypes.length + " parameters.");
            }
            return null;
        } else {
            return MethodUtils.invokeMethod(this.dynaBean, methodName, args);
        }
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
