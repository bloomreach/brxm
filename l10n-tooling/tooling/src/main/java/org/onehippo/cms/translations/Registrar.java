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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.translations.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.translations.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.translations.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.UNRESOLVED;
import static org.onehippo.cms.translations.ResourceBundleLoader.getResourceBundleLoaders;
import static org.onehippo.cms.translations.TranslationsUtils.registryKey;
import static org.onehippo.cms.translations.TranslationsUtils.registryKeyPrefix;

public class Registrar {

    private static final Logger log = LoggerFactory.getLogger(Registrar.class);

    private final File registryDir;
    private final Collection<String> locales;
    private final Registry registry;

    // Maps a file name in the register to the set of registry keys that were collected during updateRegistry()
    private final Map<String, Set<String>> registryKeysByFileName = new HashMap<>();

    public Registrar(final File registryDir, final Collection<String> locales) {
        this.registryDir = registryDir;
        this.locales = locales;
        registry = new Registry(registryDir);
    }

    public Registry getRegistry() {
        return registry;
    }

    public void initializeRegistry() throws IOException {
        // iterate over all English source bundles and register the keys as clean
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"))) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                RegistryFile registryFile = registry.loadRegistryFile(sourceBundle);
                registryFile.setBundleType(sourceBundle.getType());

                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    KeyData keyData = new KeyData(CLEAN);
                    registryFile.putKeyData(registryKey(sourceBundle, sourceKey), keyData);
                }

                registryFile.save();
            }
        }

        // iterate over all non-English source bundles and process the keys in there
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(locales)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                final RegistryFile registryFile = registry.loadRegistryFile(sourceBundle);
                final String registryKeyPrefix = registryKeyPrefix(sourceBundle);

                final Set<String> expectedKeys = registryFile.getKeys().stream()
                        .filter(str -> str.startsWith(registryKeyPrefix)).collect(Collectors.toSet());

                // collect the source keys mapped to their registry key format
                final Set<String> collectedKeys = new HashSet<>();
                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    final String registryKey = registryKey(sourceBundle, sourceKey);
                    collectedKeys.add(registryKey);
                    if (!expectedKeys.contains(registryKey)) {
                        log.warn("Resource bundle file '{}' contains key '{}' which does not have an English reference translation",
                                sourceBundle.getFileName(), sourceKey);
                    }
                }

                // register if there are any keys that do not have a translation yet
                for (String expectedKey : expectedKeys) {
                    if (!collectedKeys.contains(expectedKey)) {
                        log.info("Translation missing for key '{}' in file '{}' for locale '{}'",
                                expectedKey, sourceBundle.getFileName(), sourceBundle.getLocale());
                    }
                }

                registryFile.save();
            }
        }
    }

    public void updateRegistry() throws IOException {
        /* iterate over all source bundles:
         *  - register keys that are new in the source and not yet in the register
         *  - register keys that are updated in the source compared to the reference
         *  - update the reference
         */
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Arrays.asList("en"))) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                ResourceBundle referenceBundle = loadReference(sourceBundle);

                if (referenceBundle == null) {
                    registerNewResourceBundle(sourceBundle);
                } else {
                    registerUpdatedKeys(sourceBundle, referenceBundle);
                }

                saveReference(sourceBundle);
            }
        }

        // iterate over all registry files and mark all keys that were not in the source files as deleted
        for (RegistryFile registryFile : registry.listRegistryFiles()) {
            registryFile.load();

            Set<String> expectedKeys = getRegistryKeysForFile(registryFile);
            for (String registryKey : registryFile.getKeys()) {
                if (!expectedKeys.contains(registryKey)) {
                    KeyData data = registryFile.getKeyData(registryKey);
                    if (data == null) {
                        data = new KeyData();
                    }
                    data.setStatus(KeyData.KeyStatus.DELETED);
                    registryFile.putKeyData(registryKey, data);
                }
            }

            registryFile.save();
        }
    }
    
    private ResourceBundle loadReference(ResourceBundle sourceBundle) throws IOException {
        ResourceBundleSerializer serializer = ResourceBundleSerializer.create(registryDir, sourceBundle.getType());

        try {
            final String bundleFileName = TranslationsUtils.getLocalizedBundleFileName(
                    sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
            return serializer.deserializeBundle(bundleFileName, sourceBundle.getName(), sourceBundle.getLocale());
        } catch (FileNotFoundException fne) {
            return null;
        }
    }
    
    private Set<String> getRegistryKeysForFile(RegistryFile registryFile) {
        Set<String> keySet = registryKeysByFileName.get(registryFile.getId());
        if (keySet == null) {
            keySet = new HashSet<>();
            registryKeysByFileName.put(registryFile.getId(), keySet);
        }
        return keySet;
    }

    private void registerNewResourceBundle(final ResourceBundle sourceBundle) throws IOException {
        log.debug("Registering new bundle: {}", sourceBundle.getFileName());
        final RegistryFile registryFile = registry.loadRegistryFile(sourceBundle);
        registryFile.setBundleType(sourceBundle.getType());
        final Set<String> registryKeys = getRegistryKeysForFile(registryFile);

        for (String sourceKey : sourceBundle.getEntries().keySet()) {
            final String registryKey = registryKey(sourceBundle, sourceKey);

            if (registryFile.getKeyData(registryKey) != null) {
                log.warn("Found unexpected reference data for key {} in file {} while registering new resource bundle, resetting to status ADDED",
                        registryKey, registryFile.getId());
            }

            final KeyData keyData = new KeyData(ADDED);
            for (String locale : locales) {
                keyData.setLocaleStatus(locale, UNRESOLVED);
            }
            registryFile.putKeyData(registryKey, keyData);

            registryKeys.add(registryKey);
        }

        registryFile.save();
    }

    private void registerUpdatedKeys(final ResourceBundle sourceBundle, final ResourceBundle referenceBundle) throws IOException {
        final RegistryFile registryFile = registry.loadRegistryFile(sourceBundle);
        registryFile.setBundleType(sourceBundle.getType());
        final Set<String> registryKeys = getRegistryKeysForFile(registryFile);

        for (String sourceKey : sourceBundle.getEntries().keySet()) {
            final String sourceTranslation = sourceBundle.getEntries().get(sourceKey);
            final String referenceTranslation = referenceBundle.getEntries().get(sourceKey);
            final String registryKey = registryKey(sourceBundle, sourceKey);

            KeyData keyData = registryFile.getKeyData(registryKey);
            
            if (keyData == null) {
                if (referenceTranslation != null) {
                    log.warn("Can not find registry data for key {} in file {} while registering updated keys, resetting to status ADDED",
                            registryKey, registryFile.getId());
                }
                keyData = new KeyData(ADDED);
                registryFile.putKeyData(registryKey, keyData);
            } else {
                if (!referenceTranslation.equals(sourceTranslation)) {
                    keyData.setStatus(UPDATED);
                }
            }
            if (keyData.getStatus() != CLEAN) {
                for (String locale : locales) {
                    keyData.setLocaleStatus(locale, UNRESOLVED);
                }
            }

            registryKeys.add(registryKey);
        }

        registryFile.save();
    }

    private void saveReference(final ResourceBundle sourceBundle) throws IOException {
        final String bundleFileName = TranslationsUtils.getLocalizedBundleFileName(
                sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
        sourceBundle.setFileName(bundleFileName);
        registry.saveResourceBundle(sourceBundle);
    }

    public static void main(String[] args) throws Exception {
        final String command = args[0];
        File registryDir = new File(args[1]);
        if (!registryDir.exists()) {
            throw new IllegalStateException("Directory does no exist: " + registryDir.getPath());
        }
        final Collection<String> locales = Arrays.asList(args[2].split(","));
        final Registrar registrar = new Registrar(registryDir, locales);
        switch (command) {
            case "initialize":
                registrar.initializeRegistry();
                break;
            case "update":
                registrar.updateRegistry();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized command: " + command);
        }
    }

}
