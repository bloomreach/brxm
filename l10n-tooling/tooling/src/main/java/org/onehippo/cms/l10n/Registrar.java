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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms.l10n.KeyData.KeyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.l10n.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;
import static org.onehippo.cms.l10n.ResourceBundleLoader.getResourceBundleLoaders;
import static org.onehippo.cms.l10n.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;
import static org.onehippo.cms.l10n.TranslationsUtils.registryKey;

class Registrar {

    private static final Logger log = LoggerFactory.getLogger(Registrar.class);
    public static final String REPORT_FILE = "target/TEST-l10n-changes.xml";

    private final File baseDir;
    private final File registryDir;
    private final Collection<String> locales;
    private final Registry registry;
    private final String moduleName;
    private final ClassLoader classLoader;
    private final String[] excludes;
    
    Registrar(final File baseDir, final String moduleName, final Collection<String> locales, final ClassLoader classLoader, final String[] excludes) throws IOException {
        this.baseDir = baseDir;
        this.registryDir = new File(baseDir, "resources");
        this.locales = locales;
        registry = new Registry(registryDir);
        this.moduleName = moduleName;
        this.classLoader = classLoader;
        this.excludes = excludes;
    }

    Registry getRegistry() {
        return registry;
    }

    void addLocale(final String locale) throws IOException {
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"), classLoader, excludes)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                RegistryInfo registryInfo = registry.getRegistryInfoForBundle(sourceBundle);
                registryInfo.setBundleType(sourceBundle.getType());
                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    KeyData keyData = registryInfo.getKeyData(registryKey(sourceBundle, sourceKey));
                    if (keyData == null) {
                        String error = "Could not find registry data for key '" + registryKey(sourceBundle, sourceKey)
                                + "' for file '" + registryInfo.getFileName() + "'. Please update register before adding a locale.";
                        log.error(error);
                        throw new IllegalStateException(error);
                    } else {
                        keyData.setStatus(UPDATED);
                        keyData.setLocaleStatus(locale, UNRESOLVED);
                    }
                }
                registryInfo.save();
            }
        }
    }

    void initialize() throws IOException {
        // iterate over all english source bundles and register all translations as UNRESOLVED
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"), classLoader, excludes)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                RegistryInfo registryInfo = registry.getRegistryInfoForBundle(sourceBundle);
                registryInfo.setBundleType(sourceBundle.getType());
                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    final String registryKey = registryKey(sourceBundle, sourceKey);
                    KeyData keyData = registryInfo.getKeyData(registryKey);
                    if (keyData == null) {
                        keyData = new KeyData(UPDATED);
                        for (String locale : locales) {
                            keyData.setLocaleStatus(locale, UNRESOLVED);
                        }
                        registryInfo.putKeyData(registryKey, keyData);
                    }
                }

                registryInfo.save();
            }
        }

        // iterate over all non-english source bundles and process the keys in there
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(locales, classLoader, excludes)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                final RegistryInfo registryInfo = registry.getRegistryInfoForBundle(sourceBundle);
                ResourceBundle targetBundle = null;
                for (String sourceKey : sourceBundle.getEntries().keySet()) {
                    final KeyData keyData = registryInfo.getKeyData(registryKey(sourceBundle, sourceKey));
                    if (keyData == null) {
                        log.warn("Resource bundle '{}' contains unused key '{}': removing", sourceBundle.getId(), sourceKey);
                        if (targetBundle == null) {
                            targetBundle = loadReferenceBundle(sourceBundle);
                        }
                        targetBundle.getEntries().remove(sourceKey);
                    } else {
                        keyData.setLocaleStatus(sourceBundle.getLocale(), RESOLVED);
                    }
                }
                registryInfo.save();
                if (targetBundle != null) {
                    if (!targetBundle.isEmpty()) {
                        targetBundle.save();
                    } else {
                        targetBundle.delete();
                    }
                }
            }
        }
    }

    void update() throws IOException {
        final UpdateRegistryUpdateListener listener = new UpdateRegistryUpdateListener(registry, locales);
        scan(listener);
    }
    
    Report report() throws IOException {
        final ReportUpdateListener listener = new ReportUpdateListener(registry);
        scan(listener);
        listener.writeReport();
        return listener.writer.report;
    }

    void scan(final UpdateListener listener) throws IOException {
        final Collection<String> bundles = new ArrayList<>();
        for (ResourceBundleLoader bundleLoader : getResourceBundleLoaders(Collections.singletonList("en"), classLoader, excludes)) {
            for (ResourceBundle sourceBundle : bundleLoader.loadBundles()) {
                ResourceBundle referenceBundle = loadReferenceBundle(sourceBundle);
                listener.startBundle(sourceBundle, referenceBundle);
                if (!referenceBundle.exists()) {
                    listener.bundleAdded();
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
                        if (!sourceBundle.getEntries().containsKey(key)) {
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

    private ResourceBundle loadReferenceBundle(ResourceBundle sourceBundle) throws IOException {
        final String bundleFileName = mapSourceBundleFileToTargetBundleFile(
                sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
        ResourceBundle referenceBundle = ResourceBundle.createInstance(sourceBundle.getName(), bundleFileName, 
                new File(registryDir, bundleFileName), sourceBundle.getType());
        referenceBundle.setModuleName(moduleName);
        referenceBundle.load();
        return referenceBundle;
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

        abstract void bundleAdded() throws IOException;

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
    
    private class ReportUpdateListener extends UpdateListener {
        
        private JunitReportWriter writer = new JunitReportWriter(moduleName);
        private boolean added;

        private ReportUpdateListener(final Registry registry) {
            super(registry);
        }

        @Override
        public void startBundle(final ResourceBundle sourceBundle, final ResourceBundle referenceBundle) {
            super.startBundle(sourceBundle, referenceBundle);
            writer.startTestCase(sourceBundle.getId());
        }

        @Override
        public void bundleAdded() {
            writer.failure("Bundle added");
            added = true;
        }
        
        @Override
        public void keyAdded(final String key) throws IOException {
            final KeyData keyData = getKeyData(key);
            if (keyData == null) {
                writer.failure("Key '" + key + "' added");
            } else {
                writer.error("Expected no key data, but found " + keyData.getStatus());
            }
        }
        
        @Override
        public void keyUpdated(final String key) throws IOException {
            writer.failure("Key '" + key + "' updated");
        }
        
        @Override
        public void keyDeleted(final String key) throws IOException {
            writer.failure("Key '" + key + "' removed");
        }

        @Override
        public void bundleDeleted(final ResourceBundle resourceBundle) {
            writer.startTestCase(resourceBundle.getId());
            writer.failure("Bundle removed");
        }

        @Override
        void endBundle() throws IOException {
            if (!added && !getRegistryInfo().exists()) {
                writer.error("Missing registry info: " + getRegistryInfo().getFileName());
            }
            super.endBundle();
        }

        private void writeReport() throws IOException {
            final File file = new File(baseDir, REPORT_FILE);
            final File target = file.getParentFile();
            if (!target.exists()) {
                target.mkdir();
            }
            writer.write(file);
        }
    }
    
    private class UpdateRegistryUpdateListener extends UpdateListener {
        
        private final Collection<String> locales;
        private final Map<String, ResourceBundle> bundles = new HashMap<>();

        private UpdateRegistryUpdateListener(final Registry registry, final Collection<String> locales) {
            super(registry);
            this.locales = locales;
        }
        
        @Override
        public void bundleAdded() throws IOException {
            final RegistryInfo registryInfo = getRegistryInfo();
            registryInfo.setBundleType(currentSourceBundle.getType());
            log.debug("Adding bundle {}", currentSourceBundle.getFileName());
            for (Map.Entry<String, String> entry : currentSourceBundle.getEntries().entrySet()) {
                final String key = entry.getKey();
                final String registryKey = registryKey(currentSourceBundle, key);
                final KeyData keyData = new KeyData(ADDED);
                for (String locale : locales) {
                    keyData.setLocaleStatus(locale, UNRESOLVED);
                }
                registryInfo.putKeyData(registryKey, keyData);
                getCurrentReferenceBundle().getEntries().put(key, entry.getValue());
            }
        }
        
        @Override
        public void keyAdded(final String key) throws IOException {
            KeyData keyData = getKeyData(key);
            if (keyData != null) {
                log.error("Key added but already registered: Bundle: {}, Key: {}", currentReferenceBundle.getId(), key);
            } else {
                keyData = new KeyData(ADDED);
            }
            final RegistryInfo registryInfo = getRegistryInfo();
            log.debug("Setting status ADDED for key {} in registry file {}", key, registryInfo.getFileName());
            keyData.setStatus(ADDED);
            for (String locale : locales) {
                keyData.setLocaleStatus(locale, UNRESOLVED);
            }
            registryInfo.putKeyData(registryKey(currentSourceBundle, key), keyData);
            getCurrentReferenceBundle().getEntries().put(key, currentSourceBundle.getEntries().get(key));
        }

        @Override
        public void keyUpdated(final String key) throws IOException {
            KeyData keyData = getKeyData(key);
            final RegistryInfo registryInfo = getRegistryInfo();
            if (keyData == null) {
                log.warn("Key updated but not yet registered. Bundle: {}, Key: {}", currentSourceBundle.getId(), key);
                keyData = new KeyData(UPDATED);
                registryInfo.putKeyData(registryKey(currentSourceBundle, key), keyData);
            }
            log.debug("Setting status UPDATED for key {} in registry file {}", key, registryInfo.getFileName());
            keyData.setStatus(UPDATED);
            for (String locale : locales) {
                keyData.setLocaleStatus(locale, UNRESOLVED);
            }
            getCurrentReferenceBundle().getEntries().put(key, currentSourceBundle.getEntries().get(key));
        }

        @Override
        public void keyDeleted(final String key) throws IOException {
            final KeyData keyData = getKeyData(key);
            if (keyData == null) {
                log.warn("Key deleted but not registered. Bundle: {}, Key: {}", currentSourceBundle.getId(), key);
            }
            final RegistryInfo registryInfo = getRegistryInfo();
            for (String locale : locales) {
                final ResourceBundle resourceBundle = getResourceBundle(currentSourceBundle.getName(), locale, getRegistryInfo());
                log.debug("Deleting key {} from bundle {}", key, resourceBundle.getFileName());
                resourceBundle.getEntries().remove(key);
            }
            registryInfo.removeKeyData(key);
            getCurrentReferenceBundle().getEntries().remove(key);
        }

        @Override
        public void bundleDeleted(final ResourceBundle referenceBundle) throws IOException {
            final RegistryInfo registryInfo = registry.getRegistryInfoForBundle(referenceBundle);
            bundles.put(referenceBundle.getLocale(), referenceBundle);
            for (String locale : locales) {
                final ResourceBundle resourceBundle = registry.getResourceBundle(referenceBundle.getName(), locale, registryInfo);
                log.debug("Deleting bundle {}", resourceBundle.getId());
                resourceBundle.delete();
            }
            for (Map.Entry<String, String> entry : referenceBundle.getEntries().entrySet()) {
                registryInfo.removeKeyData(registryKey(referenceBundle, entry.getKey()));
            }
            log.debug("Deleting bundle {}", referenceBundle.getId());
            referenceBundle.delete();
            if (registryInfo.getKeys().isEmpty()) {
                registryInfo.delete();
            } else {
                registryInfo.save();
            }
        }

        @Override
        void endBundle() throws IOException {
            if (!bundles.isEmpty()) {
                for (Map.Entry<String, ResourceBundle> entry : bundles.entrySet()) {
                    final ResourceBundle resourceBundle = entry.getValue();
                    if (resourceBundle.isEmpty()) {
                        resourceBundle.delete();
                    } else {
                        resourceBundle.save();
                    }
                }
                final RegistryInfo registryInfo = getRegistryInfo();
                if (registryInfo.getKeys().isEmpty()) {
                    registryInfo.delete();
                } else {
                    registryInfo.save();
                }
                bundles.clear();
            }
            super.endBundle();
        }

        private ResourceBundle getResourceBundle(final String name, final String locale, final RegistryInfo registryInfo) throws IOException {
            ResourceBundle resourceBundle = bundles.get(locale);
            if (resourceBundle == null) {
                resourceBundle = registry.getResourceBundle(name, locale, registryInfo);
                resourceBundle.setModuleName(moduleName);
                bundles.put(locale, resourceBundle);
            }
            return resourceBundle;
        }
        
        private ResourceBundle getCurrentReferenceBundle() throws IOException {
            return getResourceBundle(currentSourceBundle.getName(), currentSourceBundle.getLocale(), getRegistryInfo());
        }
    }
}
