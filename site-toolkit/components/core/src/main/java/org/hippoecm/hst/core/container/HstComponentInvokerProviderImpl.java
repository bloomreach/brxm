package org.hippoecm.hst.core.container;

import java.util.HashMap;
import java.util.Map;

public class HstComponentInvokerProviderImpl implements HstComponentInvokerProvider {
    
    protected Map<String, HstComponentInvoker> invokerMap = new HashMap<String, HstComponentInvoker>();

    public HstComponentInvoker getComponentInvoker(String contextName) {
        return this.invokerMap.get(contextName);
    }

    public void registerComponentInvoker(String contextName, HstComponentInvoker invoker) {
        this.invokerMap.put(contextName, invoker);
    }

    public void unregisterComponentInvoker(String contextName) {
        this.invokerMap.remove(contextName);
    }

}
