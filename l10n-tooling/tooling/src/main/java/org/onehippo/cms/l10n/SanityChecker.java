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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanityChecker {

    private static final Logger log = LoggerFactory.getLogger(SanityChecker.class);

    private final File baseDir;
    private final Collection<String> locales;

    public SanityChecker(final File baseDir, final Collection<String> locales) {
        this.baseDir = baseDir;
        this.locales = locales;
    }

    void _check() throws IOException {
        for (Module module : new ModuleLoader(baseDir).loadModules()) {
            final Registry registry = module.getRegistry();
            for (final RegistryInfo registryInfo : registry.getRegistryInfos()) {
                registryInfo.load();
                for (final String key : registryInfo.getKeys()) {
                    for (final String locale : locales) {
                        if (registryInfo.getKeyData(key).getLocaleStatus(locale) == KeyData.LocaleStatus.RESOLVED) {
                            Translation translation = new Translation(module, registryInfo, key, locale);
                            if (!SanityChecker.containSameSubstitutionPatterns(translation.getReferenceValue(), translation.getTranslation())) {
                                log.warn(
                                        translation.getFQKey()
                                                + " does not contain same substitution patterns for locale '" + locale
                                                + "' reference='" + translation.getReferenceValue()
                                                + "' translation='" + translation.getTranslation() + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    protected static boolean containSameSubstitutionPatterns(final String one, final String two) {
        final String[] onePatterns = extractSubstitutionPatterns(one);
        final String[] twoPatterns = extractSubstitutionPatterns(two);

        if (onePatterns.length != twoPatterns.length) {
            return false;
        }

        Arrays.sort(onePatterns);
        Arrays.sort(twoPatterns);

        for (int i = 0; i < onePatterns.length; i++) {
            if (!onePatterns[i].equals(twoPatterns[i])) {
                return false;
            }
        }

        return true;
    }

    protected static String[] extractSubstitutionPatterns(final String str) {
        if (str == null) {
            return new String[0];
        }

        final Pattern pattern = Pattern.compile("\\$?\\{[^\\}]*\\}+");
        final Matcher matcher = pattern.matcher(str);
        final List<String> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches.toArray(new String[matches.size()]);
    }

}
