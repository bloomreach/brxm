/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
    private static ComponentManager componentManager;
    private static String HST_VERSION;
    private static String contextPath;

    private HstServices() {
    }

    public static void setContextPath(final String contextPath) {
        HstServices.contextPath = contextPath;
    }

    public static String getContextPath() {
        return contextPath;
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
     * @return Returns the {@link HstRequestProcessor} component to serve requests.
     */
    public static HstRequestProcessor getRequestProcessor() {
        return componentManager.getComponent(HstRequestProcessor.class.getName());
    }
    
    public static String getImplementationVersion(){
        if(HST_VERSION != null) {
            return HST_VERSION;
        }
        InputStream istream = null;
        try {
            StringBuffer sb = new StringBuffer();
            String[] classElements = HstServices.class.getName().split("\\.");
            for (int i=0; i<classElements.length-1; i++) {
                sb.append("../");
            }
            sb.append("META-INF/MANIFEST.MF");
            URL classResource = HstServices.class.getResource(classElements[classElements.length-1]+".class");
            URL manifestURL = new URL(classResource, new String(sb));
            istream = manifestURL.openStream();
            if (istream != null) {
                Manifest manifest = new Manifest(istream);
                Attributes atts = manifest.getMainAttributes();
                if (atts.getValue("Implementation-Version") != null) {
                    HST_VERSION = atts.getValue("Implementation-Version");
                    return HST_VERSION;
                }
            }
        } catch (MalformedURLException ex) {
           log.warn("Cannot get HST Version", ex);
        }
        catch (IOException ex) {
            log.warn("Cannot get HST Version: {}", ex.getMessage());
        } finally {
            if(istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                   //
                }
            }
        }
        HST_VERSION = "Undefined";
        return HST_VERSION;
    }

}
