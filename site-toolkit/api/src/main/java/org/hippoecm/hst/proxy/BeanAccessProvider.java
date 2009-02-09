package org.hippoecm.hst.proxy;

public interface BeanAccessProvider {
    
    Object getProperty(String namespacePrefix, String name, Class returnType);
    
    Object setProperty(String namespacePrefix, String name, Object value, Class returnType);
    
    Object invoke(String namespacePrefix, String name, Object [] args, Class returnType);
    
}
