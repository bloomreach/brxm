package org.hippoecm.hst.service;

import javax.jcr.Node;

import org.hippoecm.hst.proxy.ProxyUtils;

public class ServiceFactory {
    
    public static <T> T create(Node n, Class clazz) {
        
        Service s = new AbstractJCRService(n){
            public Service[] getChildServices() {
                return null;
            }
            
        };
        try {
            return (T) ProxyUtils.createBeanAccessProviderProxy(new ServiceBeanAccessProviderImpl(s), clazz);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } 
        return null;
    }
}
