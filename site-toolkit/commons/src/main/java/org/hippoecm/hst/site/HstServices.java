/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site;

import org.hippoecm.hst.core.container.ComponentManager;

/**
 * A static accessor to the {@link ComponentManager} managed by the HST container.
 *
 */
public class HstServices {
    private static boolean available;
    private static ComponentManager componentManager;
    private static String HST_VERSION;

    private HstServices() {
    }

    /**
     * Sets the component manager of the HST container.
     * @param compManager the component manger of the HST container
     */
    public static void setComponentManager(ComponentManager compManager) {
        HstServices.componentManager = compManager;
        HstServices.available = (HstServices.componentManager != null);
    }
    
    /**
     * @return Returns the component manager of the HST container.
     */
    public static ComponentManager getComponentManager() {
        return HstServices.componentManager;
    }
    
    /**
     * @return Returns the flag if the HST container is available or not to serve requests.
     */
    public static boolean isAvailable() {
        return HstServices.available;
    }

    public static String getImplementationVersion() {
        if (HST_VERSION != null) {
            return HST_VERSION;
        }

        final String implVersion = HstServices.class.getPackage().getImplementationVersion();

        if (implVersion != null) {
            HST_VERSION = implVersion;
        } else {
            HST_VERSION = "Undefined";
        }

        return HST_VERSION;
    }

}
