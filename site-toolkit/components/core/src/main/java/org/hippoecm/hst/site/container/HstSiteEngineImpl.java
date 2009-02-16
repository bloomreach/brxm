package org.hippoecm.hst.site.container;

import java.util.Properties;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstSiteEngine;
import org.hippoecm.hst.site.HstServices;

public class HstSiteEngineImpl implements HstSiteEngine {

    protected ComponentManager componentManager;

    public HstSiteEngineImpl() {
        this(null);
    }
    
    public HstSiteEngineImpl(Properties initProps) {
        this.componentManager = new SpringComponentManager(initProps);
    }

    public void start() throws ContainerException {
        this.componentManager.initialize();
        this.componentManager.start();
        HstServices.setComponentManager(this.componentManager);
    }

    public void shutdown() throws ContainerException {
        this.componentManager.stop();
        this.componentManager.close();
        HstServices.setComponentManager(null);
    }

    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

}
