package org.onehippo.cms7.crisp.hst.module;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

public class CrispHstServices {

    public static final String MODULE_NAME = "org.onehippo.cms7.crisp.hst";

    private CrispHstServices() {
    }

    public static <T> T getModuleComponent(String name) {
        if (!HstServices.isAvailable()) {
            throw new IllegalStateException("HST Services unavailable.");
        }

        ComponentManager compMgr = HstServices.getComponentManager();

        return compMgr.getComponent(name, MODULE_NAME);
    }

    public static ResourceServiceBroker getDefaultResourceServiceBroker() {
        return getModuleComponent(ResourceServiceBroker.class.getName());
    }
}
