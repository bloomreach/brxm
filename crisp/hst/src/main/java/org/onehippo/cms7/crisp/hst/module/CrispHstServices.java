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
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

/**
 * Convenient utility to allow easy access to the singleton {@link ResourceServiceBroker} in a Content Deliery tier
 * application.
 * <P>
 * <EM>Note</EM>: If you need to access the singleton {@link ResourceServiceBroker} in a Content Authoring tier
 * or any other applications (including Content Delivery tier applications), you can use the following technique
 * instead:
 * </P>
 * <PRE>
 * ResourceServiceBroker broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
 * </PRE>
 */
public class CrispHstServices {

    /**
     * CRISP HST Addon Module name.
     */
    public static final String MODULE_NAME = "org.onehippo.cms7.crisp.hst";

    private CrispHstServices() {
    }

    /**
     * Returns a component registered in CRISP HST Addon Module's {@link ComponentManager}.
     * @param name a component bean name registered in CRISP HST Addon Module's {@link ComponentManager}
     * @return a component registered in CRISP HST Addon Module's {@link ComponentManager}
     */
    public static <T> T getModuleComponent(String name) {
        if (!HstServices.isAvailable()) {
            throw new IllegalStateException("HST Services unavailable.");
        }

        ComponentManager compMgr = HstServices.getComponentManager();

        return compMgr.getComponent(name, MODULE_NAME);
    }

    /**
     * Returns the singleton {@link ResourceServiceBroker} instance.
     * @return the singleton {@link ResourceServiceBroker} instance
     */
    public static ResourceServiceBroker getDefaultResourceServiceBroker() {
        return getModuleComponent(ResourceServiceBroker.class.getName());
    }
}
