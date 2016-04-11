/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translation {
    
    private static final Logger log = LoggerFactory.getLogger(Translation.class);
    
    private final Module module;
    private final RegistryFile registryFile;
    private final String key;
    private final String locale;
    private final String translation;
    
    public Translation(final Module module, final RegistryFile registryFile, final String key, final String locale) {
        this(module, registryFile, key, locale, null);
    }

    public Translation(final Module module, final RegistryFile registryFile, final String key, final String locale, final String translation) {
        this.module = module;
        this.registryFile = registryFile;
        this.key = key;
        this.locale = locale;
        this.translation = translation;
    }
    
    public Module getModule() {
        return module;
    }
    
    public RegistryFile getRegistryFile() {
        return registryFile;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getFQKey() {
        return module.getName() + "#" + getRegistryFile().getId() + "#" + key;
    }
    
    private String getBundleKey() {
        final int offset = key.lastIndexOf('/');
        if (offset != -1) {
            return key.substring(offset+1);
        }
        return key;
    }
    
    private String getBundleName() {
        final int offset = key.lastIndexOf('/');
        if (offset != -1) {
            return key.substring(0, offset);
        }
        return null;
    }
    
    public String getReferenceValue() {
        return getBundleValue("en");
    }
    
    public String getTranslation() {
        if (translation != null) {
            return translation;
        }
        return getBundleValue(locale);
    }
    
    private String getBundleValue(final String locale) {
        try {
            final ResourceBundle resourceBundle = module.getRegistry().loadResourceBundle(getBundleName(), locale, registryFile);
            if (resourceBundle != null) {
                return resourceBundle.getEntries().get(getBundleKey());
            }
        } catch (IOException e) {
            log.error("Failed to load bundle corresponding to registry file {} in locale {}", registryFile.getId(), locale, e);
        }
        return null;

    }
}
