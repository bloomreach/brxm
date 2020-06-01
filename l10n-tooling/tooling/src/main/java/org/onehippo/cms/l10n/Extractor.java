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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.l10n.ResourceBundleLoader.getResourceBundleLoaders;
import static org.onehippo.cms.l10n.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;

public class Extractor {
    
    private static final Logger log = LoggerFactory.getLogger(Extractor.class);

    private final File registryDir;
    private final Collection<String> locales;
    private final String moduleName;
    private final ClassLoader classLoader;
    private final String[] excludes;
    
    public Extractor(final File registryDir, final String moduleName, final Collection<String> locales, final ClassLoader classLoader, final String[] excludes) {
        this.registryDir = registryDir;
        this.locales = locales;
        this.moduleName = moduleName;
        this.classLoader = classLoader;
        this.excludes = excludes;
    }
    
    public void extract() throws IOException {
        for (ResourceBundleLoader loader : getResourceBundleLoaders(locales, classLoader, excludes)) {
            for (ResourceBundle sourceBundle : loader.loadBundles()) {
                final String bundleFileName = mapSourceBundleFileToTargetBundleFile(
                        sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
                final ResourceBundle targetBundle = ResourceBundle.createInstance(
                        sourceBundle.getName(), bundleFileName, new File(registryDir, bundleFileName), sourceBundle.getType());
                targetBundle.setModuleName(moduleName);
                if (targetBundle.exists()) {
                    continue;
                }
                for (Map.Entry<String, String> entry : sourceBundle.getEntries().entrySet()) {
                    targetBundle.getEntries().put(entry.getKey(), entry.getValue());
                }
                if (!targetBundle.isEmpty()) {
                    targetBundle.save();
                } else {
                    log.warn("Not saving empty bundle: {}", targetBundle.getId());
                }
            }
        }
    }

}
