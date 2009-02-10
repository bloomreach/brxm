package org.hippoecm.hst.service;

public interface ServiceBeanAccessProvider {
    
    Object getProperty(String namespacePrefix, String name, Class returnType);
    
    Object setProperty(String namespacePrefix, String name, Object value, Class returnType);
    
    Object invoke(String namespacePrefix, String name, Object [] args, Class returnType);
    
}
