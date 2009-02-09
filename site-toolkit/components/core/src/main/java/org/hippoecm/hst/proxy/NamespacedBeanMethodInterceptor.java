package org.hippoecm.hst.proxy;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hippoecm.hst.service.ServiceNamespace;

public class NamespacedBeanMethodInterceptor implements MethodInterceptor {
    
    private BeanAccessProvider provider;
    private String defaultNamespacePrefix;
    
    public NamespacedBeanMethodInterceptor(final BeanAccessProvider provider, String defaultNamespacePrefix) {
        this.provider = provider;
        this.defaultNamespacePrefix = defaultNamespacePrefix;
    }
    
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        String namespacePrefix = getNamespacePrefix(method);
        String methodName = method.getName();
        Class [] paramTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        Object [] args = invocation.getArguments();
        
        if (methodName.startsWith("get") && paramTypes.length == 0) {
            String propName = getCamelString(methodName.substring(3));
            return provider.getProperty(namespacePrefix, propName, returnType);
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            String propName = getCamelString(methodName.substring(2));
            return provider.getProperty(namespacePrefix, propName, returnType);
        } else if (methodName.startsWith("set") && paramTypes.length == 1) {
            String propName = getCamelString(methodName.substring(3));
            return provider.setProperty(namespacePrefix, propName, args[0], returnType);
        } else {
            return provider.invoke(namespacePrefix, methodName, args, returnType);
        }
    }
    
    private String getNamespacePrefix(Method method) {
        String namespacePrefix = this.defaultNamespacePrefix;
        
        if (method.isAnnotationPresent(ServiceNamespace.class)) {
            namespacePrefix = method.getAnnotation(ServiceNamespace.class).prefix();
        }
        
        return namespacePrefix;
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
