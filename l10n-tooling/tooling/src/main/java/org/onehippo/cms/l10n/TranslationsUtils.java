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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.LocaleUtils;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.onehippo.cms.l10n.BundleType.ANGULAR;
import static org.onehippo.cms.l10n.BundleType.REPOSITORY;
import static org.onehippo.cms.l10n.BundleType.WICKET;

public final class TranslationsUtils {

    static final String REGISTRY_FILE_SUFFIX = "registry.json";
    private static final String REPOSITORY_RESOURCE_BUNDLE_EXT = ".yaml";

    private TranslationsUtils() {}
    
    public static String mapSourceBundleFileToTargetBundleFile(String sourceBundleFileName, BundleType bundleType, String locale) {
        switch (bundleType) {
            case REPOSITORY: {
                return substringBefore(sourceBundleFileName, REPOSITORY_RESOURCE_BUNDLE_EXT) + "_" + locale + REPOSITORY_RESOURCE_BUNDLE_EXT;
            }
            case WICKET: {
                return sourceBundleFileName;
            }
            case ANGULAR: {
                return sourceBundleFileName;
            }
        }
        throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
    }
    
    public static String registryKey(final ResourceBundle resourceBundle, final String bundleKey) {
        return registryKeyPrefix(resourceBundle) + bundleKey;
    }
    
    public static String registryKeyPrefix(final ResourceBundle resourceBundle) {
        if (resourceBundle.getType() == BundleType.REPOSITORY) {
            return resourceBundle.getName() + "/";
        }
        return "";
    }

    public static String mapRegistryFileToResourceBundleFile(final RegistryInfo registryInfo, final String locale) {
        return mapRegistryFileToResourceBundleFile(registryInfo.getFileName(), registryInfo.getBundleType(), locale);
    }
    
    public static String mapRegistryFileToResourceBundleFile(String registryFileName, BundleType bundleType, String locale) {
        switch (bundleType) {
            case ANGULAR: {
                return substringBefore(registryFileName, REGISTRY_FILE_SUFFIX) + locale + ".json";
                
            }
            case REPOSITORY: {
                return substringBefore(registryFileName, "." + REGISTRY_FILE_SUFFIX) + "_" + locale + REPOSITORY_RESOURCE_BUNDLE_EXT;
            }
            case WICKET: {
                final String baseName = substringBefore(registryFileName, "." + REGISTRY_FILE_SUFFIX);
                if (locale.equals("en")) {
                    return baseName + ".properties";
                } else {
                    return baseName + "_" + locale + ".properties";
                }
            }
        }
        throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
    }
    
    public static String mapResourceBundleToRegistryInfoFile(final BundleType bundleType, final String bundleFileName) {
        switch (bundleType) {
            case ANGULAR:
                return substringBeforeLast(bundleFileName, "/")
                        + "/" + REGISTRY_FILE_SUFFIX;
            case REPOSITORY: {
                String baseName = substringBefore(bundleFileName, REPOSITORY_RESOURCE_BUNDLE_EXT);
                String locale = substringAfterLast(baseName, "_");
                try {
                    // check if postfixed with locale string
                    LocaleUtils.toLocale(locale);
                    baseName = substringBeforeLast(baseName, "_");
                } catch (IllegalArgumentException ignored) {
                }
                return baseName + "." + REGISTRY_FILE_SUFFIX;
            }
            case WICKET: {
                String baseName = substringBefore(bundleFileName, ".properties");
                String locale = substringAfterLast(baseName, "_");
                try {
                    // check if postfixed with locale string
                    LocaleUtils.toLocale(locale);
                    baseName = substringBeforeLast(baseName, "_");
                } catch (IllegalArgumentException ignored) {
                }
                return baseName + "." + REGISTRY_FILE_SUFFIX;
            }
        }
        throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
    }

    public static String mapResourceBundleToRegistryInfoFile(final ResourceBundle resourceBundle) {
        return mapResourceBundleToRegistryInfoFile(resourceBundle.getType(), resourceBundle.getFileName());
    }

    public static String mapResourceBundleToRegistryInfoFile(final String resourceBundleFile) {
        BundleType bundleType;
        if (resourceBundleFile.endsWith("/en.json")) {
            bundleType = ANGULAR;
        } else if (resourceBundleFile.endsWith(REPOSITORY_RESOURCE_BUNDLE_EXT)) {
            bundleType = REPOSITORY;
        } else if (resourceBundleFile.endsWith(".properties")) {
            bundleType = WICKET;
        } else {
            throw new IllegalArgumentException("Failed determine bundle type from bundle file name: " + resourceBundleFile);
        }
        return mapResourceBundleToRegistryInfoFile(bundleType, resourceBundleFile);
    }

    public static String getLocaleFromBundleFileName(final String bundleFileName, final BundleType bundleType) {
        switch (bundleType) {
            case ANGULAR: {
                return substringBefore(substringAfterLast(bundleFileName, "/"), ".json");
            }
            case REPOSITORY: {
                final String locale = substringBefore(substringAfterLast(bundleFileName, "_"), REPOSITORY_RESOURCE_BUNDLE_EXT);
                try {
                    LocaleUtils.toLocale(locale);
                    return locale;
                } catch (IllegalArgumentException e) {
                    return "en";
                }
            }
            case WICKET: {
                final String locale = substringBefore(substringAfterLast(bundleFileName, "_"), ".properties");
                try {
                    LocaleUtils.toLocale(locale);
                    return locale;
                } catch (IllegalArgumentException e) {
                    return "en";
                }
            }
        }
        throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
    }
    
    public static Map<String, String> propertiesToMap(final Properties properties) {
        final Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }
    
    public static void checkLocales(final Collection<String> locales) throws IllegalArgumentException {
        if (locales.isEmpty()) {
            throw new IllegalArgumentException("No locales specified");
        }
        for (String locale : locales) {
            LocaleUtils.toLocale(locale);
        }
    }
}
