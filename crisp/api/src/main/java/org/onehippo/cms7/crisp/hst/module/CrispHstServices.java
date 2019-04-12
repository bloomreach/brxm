/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.hst.module;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ModuleNotFoundException;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenient utility to allow easy access to the singleton {@link ResourceServiceBroker} in an application.
 * <P>
 * <EM>Note:</EM>
 * Each set of the static members of <code>org.hippoecm.hst.site.HstServices</code> in multiple wars has its own
 * {@link ComponentManager}. So, the {@link ComponentManager} of each application can be used to try to find the
 * local CRISP {@link ResourceServiceBroker} component. If not found from the local {@link ComponentManager}, then
 * it can try to find it from {@link HippoServiceRegistry} as fallback.
 */
public class CrispHstServices {

    private static Logger log = LoggerFactory.getLogger(CrispHstServices.class);

    /**
     * CRISP HST Addon Module name.
     */
    public static final String MODULE_NAME = "org.onehippo.cms7.crisp.hst";

    private CrispHstServices() {
    }

    /**
     * Returns a component registered in CRISP HST Addon Module's {@link ComponentManager}.
     * @param componentManager the {@link ComponentManager} that initialized the CRISP HST Addon Module, normally
     *        gotten from <code>HstServices.getComponentManager()</code>
     * @param componentName component name
     * @return a component registered in CRISP HST Addon Module's {@link ComponentManager}
     */
    public static <T> T getModuleComponent(final ComponentManager componentManager, String componentName) {
        if (componentManager == null) {
            throw new IllegalArgumentException("componentManager shouldn't be null.");
        }

        if (componentName == null) {
            throw new IllegalArgumentException("componentName shouldn't be null.");
        }

        try {
            return componentManager.getComponent(componentName, MODULE_NAME);
        } catch (ModuleNotFoundException e) {
            log.debug(
                    "HST Addon module, '{}', not found, which is ignorable as it might not be deployed onto a specific webapp.",
                    MODULE_NAME);
        }

        return null;
    }

    /**
     * Returns a component registered in CRISP HST Addon Module's {@link ComponentManager}.
     * @param name a component bean name registered in CRISP HST Addon Module's {@link ComponentManager}
     * @return a component registered in CRISP HST Addon Module's {@link ComponentManager}
     * @deprecated since 13.1.1. Use {@link #getModuleComponent(ComponentManager, String)} instead.
     */
    @Deprecated
    public static <T> T getModuleComponent(String name) {
        log.warn("Invocation on the deprecated CrispHstServices#getModuleComponent(name) since v13.1.1. "
                + "Use CrispHstServices#getModuleComponent(HstServices.getComponentManager(), name) instead.");

        final ComponentManager componentManager = HstServicesAccess.getComponentManager();

        if (componentManager == null) {
            return null;
        }

        return getModuleComponent(componentManager, name);
    }

    /**
     * Returns the singleton {@link ResourceServiceBroker} instance from either {@code componentManager} or
     * {@link HippoServiceRegistry}.
     * @param componentManager {@link ComponentManager} instance, normally gotten from <code>HstServices.getComponentManager()</code>
     * @return the singleton {@link ResourceServiceBroker} instance
     */
    public static ResourceServiceBroker getDefaultResourceServiceBroker(final ComponentManager componentManager) {
        ResourceServiceBroker broker = null;

        if (componentManager != null) {
            broker = getModuleComponent(componentManager, ResourceServiceBroker.class.getName());
        }

        if (broker == null) {
            broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        }

        return broker;
    }

    /**
     * Returns the singleton {@link ResourceServiceBroker} instance.
     * @return the singleton {@link ResourceServiceBroker} instance
     * @deprecated since 13.1.1. Use {@link #getDefaultResourceServiceBroker(ComponentManager)} instead.
     */
    @Deprecated
    public static ResourceServiceBroker getDefaultResourceServiceBroker() {
        log.warn("Invocation on the deprecated CrispHstServices#getDefaultResourceServiceBroker() since v13.1.1. "
                + "Use CrispHstServices#getDefaultResourceServiceBroker(HstServices.getComponentManager()) instead.");

        ResourceServiceBroker broker = null;

        final ComponentManager componentManager = HstServicesAccess.getComponentManager();

        if (componentManager != null) {
            broker = getModuleComponent(componentManager, ResourceServiceBroker.class.getName());
        }

        if (broker == null) {
            broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        }

        return broker;
    }
}
