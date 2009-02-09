package org.hippoecm.hst.proxy;

import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
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
    
    public static Object createBeanAccessProviderProxy(final BeanAccessProvider provider, Class ... proxyInterfaces) {
        ProxyFactory factory = new ProxyFactory(proxyInterfaces);
        
        String defaultNamespacePrefix = null;
        
        for (int i = 0; i < proxyInterfaces.length; i++) {
            if (proxyInterfaces[i].isAnnotationPresent(ServiceNamespace.class)) {
                defaultNamespacePrefix = ((ServiceNamespace) proxyInterfaces[i].getAnnotation(ServiceNamespace.class)).prefix();
                break;
            }
        }
        
        factory.addAdvice(new NamespacedBeanMethodInterceptor(provider, defaultNamespacePrefix));
        
        return factory.getProxy();
    }
    
}
