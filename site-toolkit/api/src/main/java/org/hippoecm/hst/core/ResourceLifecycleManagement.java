package org.hippoecm.hst.core;

public interface ResourceLifecycleManagement {
    
    boolean isActive();
    
    void setActive(boolean active);
    
    void registerResource(Object resource);
    
    void unregisterResource(Object resource);
    
    void disposeResource(Object resource);
    
    void disposeAllResources();
    
}
