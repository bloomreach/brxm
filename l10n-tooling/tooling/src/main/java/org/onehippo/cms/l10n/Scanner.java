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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.l10n.ResourceBundleLoader.getResourceBundleLoaders;

public class Scanner {
    
    private static Logger log = LoggerFactory.getLogger(Scanner.class);
    
    private final Collection<String> locales;
    private final ClassLoader classLoader;
    private final String[] excludes;

    public Scanner(final Collection<String> locales, final ClassLoader classLoader, final String[] excludes) {
        this.locales = locales;
        this.classLoader = classLoader;
        this.excludes = excludes;
    }
    
    private void scan() throws IOException {
        int bundleCount = 0, keyCount = 0, wordCount = 0;
        final Set<String> files = new HashSet<>();
        for (ResourceBundleLoader loader : getResourceBundleLoaders(locales, classLoader, excludes)) {
            for (ResourceBundle resourceBundle : loader.loadBundles()) {
                bundleCount++;
                keyCount += resourceBundle.getEntries().size();
                for (String value : resourceBundle.getEntries().values()) {
                    wordCount += value.split("\\s").length;
                }
                log.info(resourceBundle.getId());
                files.add(resourceBundle.getModuleName() + "/" + resourceBundle.getFileName());
            }
        }
        log.info("{} files, {} resource bundles, {} keys, {} words", files.size(), bundleCount, keyCount, wordCount);
    }
    
}
