package org.hippoecm.hst.util;

import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
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
}
