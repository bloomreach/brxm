package org.hippoecm.hst.service;

import javax.jcr.Node;

import org.hippoecm.hst.proxy.ProxyUtils;

public class ServiceFactory {
    
    public static <T> T create(Node n, Class ... proxyInterfaces) {
        
        T proxy = null;
        
        Service s = new AbstractJCRService(n) {
            public Service[] getChildServices() {
                return null;
            }
            
        };
        
        try {
            proxy = (T) ProxyUtils.createBeanAccessProviderProxy(new ServiceBeanAccessProviderImpl(s), proxyInterfaces);

            if (proxy instanceof UnderlyingServiceAware) {
                ((UnderlyingServiceAware) proxy).setUnderlyingService(s);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        
        return proxy;
        
    }
}
