/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Set;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.hippoecm.hst.content.beans.Node;

/**
 * Utility class to create proxies.
 *
 */
public class ProxyUtils
{
    private ProxyUtils()
    {
    }
    
    /**
     * Creates and returns a dynamic proxy which throws {@link java.lang.UnsupportedOperationException}
     * for some operations.
     * This can be useful if you create a proxy with some methods unsupported. 
     * 
     * @param target the proxied target object
     * @param unsupportedMethodNames
     * @param proxyInterfaces the interfaces the created proxy should implement
     * @return
     */
    public static Object createdUnsupportableProxyObject(Object target, final Set<String> unsupportedMethodNames, Class ... proxyInterfaces) {
        ProxyFactory factory = new ProxyFactory();
        
        Interceptor interceptor = new Interceptor() {

            public Object intercept(Invocation invocation) throws Throwable {
                if (unsupportedMethodNames.contains(invocation.getMethod().getName())) {
                    throw new UnsupportedOperationException("Unsupported operation: " + invocation.getMethod().getName());
                }

                return invocation.proceed();
            }
            
        };
        
        return factory.createInterceptorProxy(target.getClass().getClassLoader(), target, interceptor, proxyInterfaces);
    }
    
    private static String findPrimaryJcrType(Class [] proxyInterfaces) {
        String primaryJcrType = null;
        
        for (Class proxyInterface : proxyInterfaces) {
            if (proxyInterface.isAnnotationPresent(Node.class)) {
                primaryJcrType = ((Node) proxyInterface.getAnnotation(Node.class)).jcrType();
                break;
            }
        }
        
        if (primaryJcrType == null) {
            for (Class proxyInterface : proxyInterfaces) {
                Class [] extendingInterfaces = proxyInterface.getInterfaces();
                
                if (extendingInterfaces.length > 0) {
                    primaryJcrType = findPrimaryJcrType(extendingInterfaces);
                    
                    if (primaryJcrType != null) {
                        break;
                    }
                }
            }
        }
        
        return primaryJcrType;
    }
    
}
