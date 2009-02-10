package org.hippoecm.hst.proxy;

import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hippoecm.hst.service.ServiceBeanAccessProvider;
import org.hippoecm.hst.service.ServiceNamespace;
import org.springframework.aop.framework.ProxyFactory;

public class ProxyUtils
{
    private ProxyUtils()
    {
    }
    
    public static Object createdUnsupportableProxyObject(Object target, final Set<String> unsupportedMethodNames)
    {
        ProxyFactory factory = new ProxyFactory(target);
        
        factory.addAdvice(new MethodInterceptor()
        {
            public Object invoke(MethodInvocation invocation) throws Throwable
            {
                if (unsupportedMethodNames.contains(invocation.getMethod().getName()))
                {
                    throw new UnsupportedOperationException("Unsupported operation: " + invocation.getMethod().getName());
                }

                return invocation.proceed();
            }
        });
        
        return factory.getProxy();
    }
    
    public static Object createBeanAccessProviderProxy(final ServiceBeanAccessProvider provider, Class ... proxyInterfaces) {
        ProxyFactory factory = new ProxyFactory(proxyInterfaces);        
        String defaultNamespacePrefix = findServiceNamespacePrefix(proxyInterfaces);
        factory.addAdvice(new NamespacedBeanMethodInterceptor(provider, defaultNamespacePrefix));
        return factory.getProxy();
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
