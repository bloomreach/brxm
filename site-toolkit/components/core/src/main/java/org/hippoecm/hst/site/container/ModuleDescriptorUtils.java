/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleDescriptorUtils {
    
    private static Logger log = LoggerFactory.getLogger(ModuleDescriptorUtils.class);

    private ModuleDescriptorUtils() {
        
    }

    public static List<ModuleDefinition> collectAllModuleDefinitions() throws IOException {
        return collectAllModuleDefinitions(Thread.currentThread().getContextClassLoader());
    }

    static List<ModuleDefinition> collectAllModuleDefinitions(ClassLoader classLoader) throws IOException {
        return collectAllModuleDefinitions(classLoader, StringUtils.split(ContainerConstants.DEFAULT_ADDON_MODULE_DESCRIPTOR_PATHS, " ,"));
    }
    
    static List<ModuleDefinition> collectAllModuleDefinitions(ClassLoader classLoader, String ... moduleDescriptorResourcePaths) throws IOException {
        List<ModuleDefinition> moduleDefinitions = new ArrayList<>();
        
        for (String moduleDescriptorResourcePath : moduleDescriptorResourcePaths) {
            Enumeration<URL> moduleDescriptorURLs = classLoader.getResources(moduleDescriptorResourcePath);
    
            while (moduleDescriptorURLs.hasMoreElements()) {
                URL moduleDescriptorURL = moduleDescriptorURLs.nextElement();
                
                try {
                    log.info("Loading module descriptor from {}", moduleDescriptorURL);
                    ModuleDefinition moduleDefinition = loadModuleDefinition(moduleDescriptorURL);
                    addActualModuleDefinitions(moduleDefinition, moduleDefinitions);
                } catch (Exception e) {
                    log.warn("Failed to load module descriptor, " + moduleDescriptorURL + ", which will be just ignored.", e);
                }
            }
        }
        
        return moduleDefinitions;
    }

    private static void addActualModuleDefinitions(final ModuleDefinition moduleDefinition, final List<ModuleDefinition> moduleDefinitions) {
        if (moduleDefinition.getName() != null) {
            // real module
            moduleDefinitions.add(moduleDefinition);
            return;
        }
        if (moduleDefinition.getModuleDefinitions() == null) {
            return;
        }
        // only a container <module> : Pick the actual real modules
        for (ModuleDefinition child : moduleDefinition.getModuleDefinitions()) {
            addActualModuleDefinitions(child, moduleDefinitions);
        }
    }

    private static ModuleDefinition loadModuleDefinition(URL url) throws JAXBException, IOException {
        ModuleDefinition moduleDefinition = null;
        
        JAXBContext jc = JAXBContext.newInstance(ModuleDefinition.class);
        Unmarshaller um = jc.createUnmarshaller();

        
        InputStream is = null;
        BufferedInputStream bis = null;
        
        try {
            is = url.openStream();
            bis = new BufferedInputStream(is);
            moduleDefinition = (ModuleDefinition) um.unmarshal(url.openStream());
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }
        
        return moduleDefinition;
    }

}
