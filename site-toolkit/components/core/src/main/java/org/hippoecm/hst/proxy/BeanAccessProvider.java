package org.hippoecm.hst.proxy;

public interface BeanAccessProvider {
    
    Object getProperty(String name, Class returnType);
    
    Object setProperty(String name, Object value, Class returnType);
    
    Object invoke(String name, Object [] args, Class returnType);
    
}
