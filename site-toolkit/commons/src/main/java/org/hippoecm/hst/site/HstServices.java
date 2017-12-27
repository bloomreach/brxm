/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static accessor to the {@link ComponentManager} managed by the HST container.
 *
 */
public class HstServices {
    private static final Logger log = LoggerFactory.getLogger(HstServices.class);
    private static boolean available;
    private static boolean hstConfigurationNodesLoaded;
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

    /**
     * @return {@code true} when the hst configuration JCR nodes have been loaded from the database into memory already
     */
    public static boolean isHstConfigurationNodesLoaded() {
        return hstConfigurationNodesLoaded;
    }

    public static void setHstConfigurationNodesLoaded(final boolean hstConfigurationNodesLoaded) {
        HstServices.hstConfigurationNodesLoaded = hstConfigurationNodesLoaded;
    }

    /**
     * @return Returns the {@link HstRequestProcessor} component to serve requests.
     * @deprecated since CMS 10.0 (hst 2.30). If needed, use
     * {@link HstServices#getComponentManager() componentManager.getComponent(HstRequestProcessor.class.getName()}
     */
    @Deprecated
    public static HstRequestProcessor getRequestProcessor() {
        return componentManager.getComponent(HstRequestProcessor.class.getName());
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
