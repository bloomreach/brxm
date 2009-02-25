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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;

public class DynaBeanMethodInterceptor implements MethodInterceptor, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private DynaBean dynaBean;
    
    public DynaBeanMethodInterceptor(final DynaBean dynaBean) {
        this.dynaBean = dynaBean;
    }
    
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        String methodName = method.getName();

        Class [] paramTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        Object [] args = invocation.getArguments();
        
        if (methodName.startsWith("get") && paramTypes.length == 0) {
            String propName = getCamelString(methodName.substring(3));
            return this.dynaBean.get(propName);
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            String propName = getCamelString(methodName.substring(2));
            return this.dynaBean.get(propName);
        } else if (methodName.startsWith("set") && paramTypes.length == 1) {
            String propName = getCamelString(methodName.substring(3));
            this.dynaBean.set(propName, args[0]);
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
