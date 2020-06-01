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

import java.util.Set;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.service.ServiceBeanAccessProvider;

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
    
    /**
     * Creates and returns a dynamic proxy which invokes the underlying service bean access provider.
     * 
     * @param provider the underlying service bean access provider
     * @param proxyInterfacesOrDelegateeClass the interfaces the proxy should implement or delegatee class which may implement interface(s).
     * @return
     * @deprecated since 2.28.05 (CMS 7.9.1). Do not use any more. No replacement
     */
    @Deprecated
    public static Object createBeanAccessProviderProxy(final ServiceBeanAccessProvider provider, Class ... proxyInterfacesOrDelegateeClass) {
        ProxyFactory factory = new ProxyFactory();
        String primaryJcrType = findPrimaryJcrType(proxyInterfacesOrDelegateeClass);
        Invoker invoker = new NamespacedBeanMethodInvoker(provider, primaryJcrType);
        Class [] proxyInterfaces = null;
        
        if (proxyInterfacesOrDelegateeClass.length == 1 && !proxyInterfacesOrDelegateeClass[0].isInterface())
        {
            proxyInterfaces = proxyInterfacesOrDelegateeClass[0].getInterfaces();
        }
        else
        {
            proxyInterfaces = proxyInterfacesOrDelegateeClass;
        }
        
        return factory.createInvokerProxy(proxyInterfaces[0].getClassLoader(), invoker, proxyInterfaces);
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
