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

import java.util.Set;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.service.ServiceBeanAccessProvider;
import org.hippoecm.hst.service.ServiceNamespace;

public class ProxyUtils
{
    private ProxyUtils()
    {
    }
    
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
    
    public static Object createBeanAccessProviderProxy(final ServiceBeanAccessProvider provider, Class ... proxyInterfaces) {
        ProxyFactory factory = new ProxyFactory();
        String defaultNamespacePrefix = findServiceNamespacePrefix(proxyInterfaces);
        Invoker invoker = new NamespacedBeanMethodInvoker(provider, defaultNamespacePrefix);
        return factory.createInvokerProxy(proxyInterfaces[0].getClassLoader(), invoker, proxyInterfaces);
    }
    
    private static String findServiceNamespacePrefix(Class [] proxyInterfaces) {
        String prefix = null;
        
        for (Class proxyInterface : proxyInterfaces) {
            if (proxyInterface.isAnnotationPresent(ServiceNamespace.class)) {
                prefix = ((ServiceNamespace) proxyInterface.getAnnotation(ServiceNamespace.class)).prefix();
                break;
            }
        }
        
        if (prefix == null) {
            for (Class proxyInterface : proxyInterfaces) {
                Class [] extendingInterfaces = proxyInterface.getInterfaces();
                
                if (extendingInterfaces.length > 0) {
                    prefix = findServiceNamespacePrefix(extendingInterfaces);
                    
                    if (prefix != null) {
                        break;
                    }
                }
            }
        }
        
        return prefix;
    }
    
}
