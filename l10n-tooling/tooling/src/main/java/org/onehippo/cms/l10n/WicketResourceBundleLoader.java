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
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.substringBefore;

class WicketResourceBundleLoader extends ResourceBundleLoader {

    private static final Logger log = LoggerFactory.getLogger(WicketResourceBundleLoader.class);

    private final String[] excludes;
    
    WicketResourceBundleLoader(final Collection<String> locales, final ClassLoader classLoader, final String[] excludes) {
        super(locales, classLoader);
        this.excludes = excludes;
    }

    @Override
    protected void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException {
        final Collection<String> entries = artifactInfo.getEntries();
        for (final String entry : entries) {
            if (entry.endsWith(".properties")) {
                if (isExcluded(entry)) {
                    log.debug("Skipping excluded properties file {}", entry);
                    continue;
                }
                final String baseName = substringBefore(entry, ".properties");
                String locale = "en";
                final int offset = baseName.lastIndexOf('_');
                if (offset != -1) {
                    try {
                        LocaleUtils.toLocale(baseName.substring(offset+1));
                        locale = baseName.substring(offset+1);
                    } catch (IllegalArgumentException ignored) {}
                }
                if (locales.contains(locale)) {
                    final Properties properties = new Properties();
                    properties.load(classLoader.getResourceAsStream(entry));
                    bundles.add(new WicketResourceBundle(entry, entry, locale, TranslationsUtils.propertiesToMap(properties)));
                }
            }
        }
    }
    
    private boolean isExcluded(final String entry) {
        for (String exclude : excludes) {
            if (exclude.equals(entry)) {
                return true;
            }
        }
        return false;
    }
}
