package org.hippoecm.hst.service;

import java.lang.reflect.Method;

public interface ServiceBeanAccessProvider {
    
    Object getProperty(String namespacePrefix, String name, Class returnType, Method method);
    
    Object setProperty(String namespacePrefix, String name, Object value, Class returnType, Method method);
    
    Object invoke(String namespacePrefix, String name, Object [] args, Class returnType, Method method);
    
}
