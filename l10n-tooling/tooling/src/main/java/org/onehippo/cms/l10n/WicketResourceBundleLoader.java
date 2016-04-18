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

import static org.apache.commons.lang.StringUtils.substringBefore;

class WicketResourceBundleLoader extends ResourceBundleLoader {

    WicketResourceBundleLoader(final Collection<String> locales) {
        super(locales);
    }

    @Override
    protected void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException {
        final Collection<String> entries = artifactInfo.getEntries();
        for (final String entry : entries) {
            if (entry.endsWith(".properties")) {
                String baseName = substringBefore(entry, ".properties");
                String locale = "en";
                final int offset = baseName.lastIndexOf('_');
                if (offset != -1) {
                    locale = baseName.substring(offset+1);
                    baseName = baseName.substring(0, offset);
                }
                if (entries.contains(baseName + ".class") && locales.contains(locale)) {
                    final Properties properties = new Properties();
                    properties.load(getClass().getClassLoader().getResourceAsStream(entry));
                    bundles.add(new WicketResourceBundle(entry, entry, locale, TranslationsUtils.propertiesToMap(properties)));
                }
            }
        }
    }

}
