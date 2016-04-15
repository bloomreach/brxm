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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms.translations.KeyData.KeyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.translations.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.translations.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.translations.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.UNRESOLVED;
import static org.onehippo.cms.translations.ResourceBundleLoader.getResourceBundleLoaders;
import static org.onehippo.cms.translations.TranslationsUtils.mapRegistryFileToResourceBundleFile;
import static org.onehippo.cms.translations.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;
import static org.onehippo.cms.translations.TranslationsUtils.registryKey;

class Registrar {

    private static final Logger log = LoggerFactory.getLogger(Registrar.class);

    private final File registryDir;
    private final Collection<String> locales;
    private final Registry registry;
    
    Registrar(final File registryDir, final Collection<String> locales) {
        this.registryDir = registryDir;
        this.locales = locales;
        registry = new Registry(registryDir);
    }

    Registry getRegistry() {
        return registry;
    }

    void initialize() throws IOException {
        // iterate over all English source bundles and register all translations as UNRESOLVED
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"))) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                RegistryInfo registryInfo = registry.getRegistryInfoForBundle(sourceBundle);
                registryInfo.setBundleType(sourceBundle.getType());

                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    KeyData keyData = new KeyData(UPDATED);
                    for (String locale : locales) {
                        keyData.setLocaleStatus(locale, UNRESOLVED);
                    }
                    registryInfo.putKeyData(registryKey(sourceBundle, sourceKey), keyData);
                }

                registryInfo.save();
            }
        }

        // iterate over all non-English source bundles and process the keys in there
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(locales)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                final RegistryInfo registryInfo = registry.getRegistryInfoForBundle(sourceBundle);

                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    final KeyData keyData = registryInfo.getKeyData(registryKey(sourceBundle, sourceKey));

                    if (keyData == null) {
                        log.warn("Resource bundle file '{}' contains key '{}' which does not have an English reference translation",
                                sourceBundle.getFileName(), sourceKey);
                    } else {
                        keyData.setLocaleStatus(sourceBundle.getLocale(), RESOLVED);
                    }
                }

                registryInfo.save();
            }
        }
    }

    void update() throws IOException {
        final UpdateRegistryUpdateListener listener = new UpdateRegistryUpdateListener(registry, locales);
        scan(listener);
    }
    
    void scan(final UpdateListener listener) throws IOException {
        final Collection<String> bundles = new ArrayList<>();
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"))) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                ResourceBundle referenceBundle = loadReferenceBundle(sourceBundle);
                listener.startBundle(sourceBundle, referenceBundle);
                if (!referenceBundle.exists()) {
                    referenceBundle = listener.bundleAdded();
                } else {
                    for (Map.Entry<String, String> entry : sourceBundle.getEntries().entrySet()) {
                        final String key = entry.getKey();
                        final String referenceValue = referenceBundle.getEntries().get(key);
                        if (referenceValue == null) {
                            listener.keyAdded(key);
                        } else if (!referenceValue.equals(entry.getValue())) {
                            listener.keyUpdated(key);
                        }
                    }
                    for (Map.Entry<String, String> entry : new HashMap<>(referenceBundle.getEntries()).entrySet()) {
                        final String key = entry.getKey();
                        final String sourceValue = sourceBundle.getEntries().get(key);
                        if (sourceValue == null) {
                            listener.keyDeleted(key);
                        }
                    }
                }
                bundles.add(referenceBundle.getId());
                listener.endBundle();
            }
        }
        for (ResourceBundle resourceBundle : registry.getReferenceBundles()) {
            if (!bundles.contains(resourceBundle.getId())) {
                listener.bundleDeleted(resourceBundle);
            }
        }
    }
    
    private void report() throws IOException {
        final ReportUpdateListener listener = new ReportUpdateListener(registry);
        scan(listener);
        listener.writeReport();
    }
    
    private ResourceBundle loadReferenceBundle(ResourceBundle sourceBundle) throws IOException {
        final String bundleFileName = mapSourceBundleFileToTargetBundleFile(
                sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
        ResourceBundle referenceBundle = ResourceBundle.createInstance(sourceBundle.getName(), bundleFileName, 
                new File(registryDir, bundleFileName), sourceBundle.getType());
        referenceBundle.load();
        return referenceBundle;
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
                registrar.initialize();
                break;
            case "update":
                registrar.update();
                break;
            case "report":
                registrar.report();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized command: " + command);
        }
    }

    private static abstract class UpdateListener {
        
        protected final Registry registry;
        protected ResourceBundle currentSourceBundle;
        protected ResourceBundle currentReferenceBundle;
        protected RegistryInfo registryInfo;

        protected UpdateListener(final Registry registry) {
            this.registry = registry;
        }

        protected RegistryInfo getRegistryInfo() throws IOException {
            if (registryInfo == null) {
                registryInfo = registry.getRegistryInfoForBundle(currentSourceBundle);
            }
            return registryInfo;
        }

        protected KeyData getKeyData(final String key) throws IOException {
            final RegistryInfo registryInfo = getRegistryInfo();
            return registryInfo.getKeyData(registryKey(currentSourceBundle, key));
        }

        protected KeyStatus getKeyStatus(final String key) throws IOException {
            final KeyData keyData = getKeyData(key);
            return keyData == null ? null : keyData.getStatus();
        }
        
        void startBundle(ResourceBundle sourceBundle, ResourceBundle referenceBundle) {
            currentSourceBundle = sourceBundle;
            currentReferenceBundle = referenceBundle;
        }

        abstract ResourceBundle bundleAdded() throws IOException;

        abstract void keyAdded(String key) throws IOException;

        abstract void keyUpdated(String key) throws IOException;

        abstract void keyDeleted(String key) throws IOException;

        abstract void bundleDeleted(ResourceBundle resourceBundle) throws IOException;

        void endBundle() throws IOException {
            currentSourceBundle = null;
            currentReferenceBundle = null;
            registryInfo = null;
        }
        
    }
    
    private static class ReportUpdateListener extends UpdateListener {
        
        private JunitReportWriter writer = new JunitReportWriter();

        private ReportUpdateListener(final Registry registry) {
            super(registry);
        }

        @Override
        public void startBundle(final ResourceBundle sourceBundle, final ResourceBundle referenceBundle) {
            super.startBundle(sourceBundle, referenceBundle);
            writer.startTestCase(sourceBundle.getId());
        }

        @Override
        public ResourceBundle bundleAdded() {
            writer.failure("Bundle added");
            return null;
        }
        
        @Override
        public void keyAdded(final String key) throws IOException {
            final KeyData keyData = getKeyData(key);
            if (keyData == null) {
                writer.failure("Key added: " + key);
            } else {
                writer.error("Expected no key data, but found " + keyData.getStatus());
            }
        }
        
        @Override
        public void keyUpdated(final String key) throws IOException {
            final KeyStatus keyStatus = getKeyStatus(key);
            if (keyStatus == CLEAN) {
                writer.failure("Key updated: " + key);
            } else {
                writer.error("Expected key status CLEAN, but found " + keyStatus);
            }
        }
        
        @Override
        public void keyDeleted(final String key) throws IOException {
            final KeyStatus keyStatus = getKeyStatus(key);
            if (keyStatus == CLEAN) {
                writer.failure("Key removed: " + key);
            } else {
                writer.error("Expected key status CLEAN, but found " + keyStatus);
            }
        }

        @Override
        public void bundleDeleted(final ResourceBundle resourceBundle) {
            writer.startTestCase(resourceBundle.getId());
            writer.failure("Bundle removed");
        }
        
        private void writeReport() throws IOException {
            writer.write(new File("target/TEST-update.xml"));
        }
    }
    
    private static class UpdateRegistryUpdateListener extends UpdateListener {
        
        private boolean updated = false;
        private final Collection<String> locales;

        private UpdateRegistryUpdateListener(final Registry registry, final Collection<String> locales) {
            super(registry);
            this.locales = locales;
        }

        @Override
        public ResourceBundle bundleAdded() throws IOException {
            final RegistryInfo registryInfo = getRegistryInfo();
            registryInfo.setBundleType(currentSourceBundle.getType());
            final ResourceBundle referenceBundle = getCurrentReferenceBundle();
            log.debug("Adding bundle {}", currentSourceBundle.getFileName());
            for (Map.Entry<String, String> entry : currentSourceBundle.getEntries().entrySet()) {
                final String key = entry.getKey();
                final String registryKey = registryKey(currentSourceBundle, key);
                final KeyData keyData = new KeyData(ADDED);
                for (String locale : locales) {
                    keyData.setLocaleStatus(locale, UNRESOLVED);
                }
                registryInfo.putKeyData(registryKey, keyData);
                referenceBundle.getEntries().put(key, entry.getValue());
            }
            updated = true;
            return referenceBundle;
        }
        
        @Override
        public void keyAdded(final String key) throws IOException {
            KeyData keyData = getKeyData(key);
            if (keyData != null) {
                log.error("Key added but already registered: Bundle: {}, Key: {}", currentReferenceBundle.getId(), key);
            } else {
                keyData = new KeyData(ADDED);
            }
            log.debug("Setting status ADDED for key {} in registry file {}", key, getRegistryInfo().getFileName());
            keyData.setStatus(ADDED);
            for (String locale : locales) {
                keyData.setLocaleStatus(locale, UNRESOLVED);
            }
            getRegistryInfo().putKeyData(registryKey(currentSourceBundle, key), keyData);
            getCurrentReferenceBundle().getEntries().put(key, currentSourceBundle.getEntries().get(key));
            updated = true;
        }

        @Override
        public void keyUpdated(final String key) throws IOException {
            KeyData keyData = getKeyData(key);
            if (keyData == null) {
                log.warn("Key updated but not yet registered. Bundle: {}, Key: {}", currentSourceBundle.getId(), key);
                keyData = new KeyData(UPDATED);
                getRegistryInfo().putKeyData(registryKey(currentSourceBundle, key), keyData);
            }
            log.debug("Setting status UPDATED for key {} in registry file {}", key, getRegistryInfo().getFileName());
            keyData.setStatus(UPDATED);
            for (String locale : locales) {
                keyData.setLocaleStatus(locale, UNRESOLVED);
            }
            getCurrentReferenceBundle().getEntries().put(key, currentSourceBundle.getEntries().get(key));
            updated = true;
        }

        @Override
        public void keyDeleted(final String key) throws IOException {
            final KeyData keyData = getKeyData(key);
            if (keyData == null) {
                log.warn("Key deleted but not registered. Bundle: {}, Key: {}", currentSourceBundle.getId(), key);
            }
            for (String locale : locales) {
                final ResourceBundle resourceBundle = registry.getResourceBundle(currentSourceBundle.getName(), locale, getRegistryInfo());
                if (resourceBundle != null) {
                    log.debug("Deleting key {} from bundle {}", key, resourceBundle.getFileName());
                    resourceBundle.getEntries().remove(key);
                }
            }
            getRegistryInfo().removeKeyData(key);
            getCurrentReferenceBundle().getEntries().remove(key);
            updated = true;
        }

        @Override
        public void bundleDeleted(final ResourceBundle referenceBundle) throws IOException {
            final RegistryInfo registryInfo = registry.getRegistryInfoForBundle(referenceBundle);
            for (String locale : locales) {
                final ResourceBundle resourceBundle = registry.getResourceBundle(referenceBundle.getName(), locale, registryInfo);
                log.debug("Deleting bundle {}", resourceBundle.getFileName());
                resourceBundle.delete();
            }
            for (Map.Entry<String, String> entry : referenceBundle.getEntries().entrySet()) {
                registryInfo.removeKeyData(registryKey(referenceBundle, entry.getKey()));
            }
            log.debug("Deleting bundle {}", referenceBundle.getFileName());
            referenceBundle.delete();
            if (registryInfo.getKeys().isEmpty()) {
                log.debug("Deleting registry info file {}", registryInfo.getFileName());
                registryInfo.delete();
            }
        }
        
        @Override
        void endBundle() throws IOException {
            if (updated) {
                final ResourceBundle currentReferenceBundle = getCurrentReferenceBundle();
                if (!currentReferenceBundle.isEmpty()) {
                    currentReferenceBundle.save();
                } else {
                    currentReferenceBundle.delete();
                }
                getRegistryInfo().save();
            }
            super.endBundle();
        }
        
        private ResourceBundle getCurrentReferenceBundle() throws IOException {
            if (currentReferenceBundle == null) {
                currentReferenceBundle = registry.getResourceBundle(currentSourceBundle.getName(), currentSourceBundle.getLocale(), getRegistryInfo());
            }
            return currentReferenceBundle;
        }
    }
}
