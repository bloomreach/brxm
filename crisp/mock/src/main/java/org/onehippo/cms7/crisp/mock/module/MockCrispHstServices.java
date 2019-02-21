/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.mock.module;

import org.hippoecm.hst.mock.core.container.MockComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;

/**
 * Mocking support to override the default {@link ResourceServiceBroker} instance, which can be retrieved through
 * {@link CrispHstServices#getDefaultResourceServiceBroker()}.
 */
public class MockCrispHstServices {

    /**
     * Override the default {@link ResourceServiceBroker} instance, so that {@link CrispHstServices#getDefaultResourceServiceBroker()}
     * will return this overriden {@link ResourceServiceBroker} instance afterward.
     * @param defaultResourceServiceBroker the default {@link ResourceServiceBroker} instance
     */
    public static void setDefaultResourceServiceBroker(final ResourceServiceBroker defaultResourceServiceBroker) {
        final MockComponentManager componentManager = new MockComponentManager() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T getComponent(final String name, final String... addonModuleNames) {
                if (ResourceServiceBroker.class.getName().equals(name) && addonModuleNames != null
                        && addonModuleNames.length != 0 && CrispHstServices.MODULE_NAME.equals(addonModuleNames[0])) {
                    return (T) defaultResourceServiceBroker;
                }

                return super.getComponent(name, addonModuleNames);
            }
        };

        HstServices.setComponentManager(componentManager);
    }

}
