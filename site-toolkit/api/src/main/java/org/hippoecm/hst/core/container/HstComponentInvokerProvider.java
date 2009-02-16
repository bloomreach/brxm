package org.hippoecm.hst.core.container;

public interface HstComponentInvokerProvider {

    public HstComponentInvoker getComponentInvoker(String contextName);
    
    public void registerComponentInvoker(String contextName, HstComponentInvoker invoker);
    
    public void unregisterComponentInvoker(String contextName);
    
}
