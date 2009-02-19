package org.hippoecm.hst.service;


public interface UnderlyingServiceAware {

    Service getUnderlyingService();
    
    void setUnderlyingService(Service service);
    
}
