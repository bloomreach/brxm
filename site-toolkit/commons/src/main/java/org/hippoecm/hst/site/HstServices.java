/*
 *  Copyright 2008 Hippo.
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
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LoggerFactory;
import org.hippoecm.hst.util.NOOPLogger;

public class HstServices {
    
    private static final String LOGGER_FACTORY_COMPONENT_NAME = LoggerFactory.class.getName();
    
    private static boolean available;
    private static ComponentManager componentManager;
    private static NOOPLogger noopLogger = new NOOPLogger();

    private HstServices() {
    }
    
    public static void setComponentManager(ComponentManager compManager) {
        HstServices.componentManager = compManager;
        HstServices.available = (HstServices.componentManager != null);
    }
    
    public static ComponentManager getComponentManager() {
        return HstServices.componentManager;
    }
    
    public static boolean isAvailable() {
        return HstServices.available;
    }
    
    public static HstRequestProcessor getRequestProcessor() {
        return componentManager.getComponent(HstRequestProcessor.class.getName());
    }
    
    public static Logger getLogger(String loggerName) {
        if (isAvailable()) {
            return ((LoggerFactory) getComponentManager().getComponent(LOGGER_FACTORY_COMPONENT_NAME)).getLogger(loggerName);
        } else {
            return noopLogger;
        }
    }
    
}
