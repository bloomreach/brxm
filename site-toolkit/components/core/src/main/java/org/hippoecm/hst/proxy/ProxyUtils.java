package org.hippoecm.hst.proxy;

import java.lang.reflect.Method;
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
    
    public static Object createBeanAccessProviderProxy(Class [] proxyInterfaces, final BeanAccessProvider provider) {
        ProxyFactory factory = new ProxyFactory(proxyInterfaces);

        factory.addAdvice(new MethodInterceptor()
        {
            public Object invoke(MethodInvocation invocation) throws Throwable
            {
                Method method = invocation.getMethod();
                String methodName = method.getName();
                Class [] paramTypes = method.getParameterTypes();
                Class returnType = method.getReturnType();
                Object [] args = invocation.getArguments();
                
                if (methodName.startsWith("get") && paramTypes.length == 0) {
                    String propName = getCamelString(methodName.substring(3));
                    return provider.getProperty(propName, returnType);
                } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
                    String propName = getCamelString(methodName.substring(2));
                    return provider.getProperty(propName, returnType);
                } else if (methodName.startsWith("set") && paramTypes.length == 1) {
                    String propName = getCamelString(methodName.substring(3));
                    return provider.setProperty(propName, args[0], returnType);
                } else {
                    return provider.invoke(methodName, args, returnType);
                }
            }
        });
        
        return factory.getProxy();
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
