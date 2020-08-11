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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.onehippo.cms.l10n.TranslationsUtils.mapRegistryFileToResourceBundleFile;
import static org.onehippo.cms.l10n.TranslationsUtils.mapResourceBundleToRegistryInfoFile;

public class Mover {

    private static final Logger log = LoggerFactory.getLogger(Mover.class);

    private final Collection<String> locales;
    private final String moduleName;
    private final Registry registry;

    Mover(final File baseDir, final String moduleName, final Collection<String> locales) throws IOException {
        this.locales = locales;
        this.moduleName = moduleName;
        registry = new Registry(new File(baseDir, "resources"));
    }

    void moveBundle(final String srcPath, final String destPath) throws IOException {
        final String srcInfoFileName = mapResourceBundleToRegistryInfoFile(srcPath);
        final String destInfoFileName = mapResourceBundleToRegistryInfoFile(destPath);
        final RegistryInfo srcRegistryInfo = registry.getRegistryInfo(srcInfoFileName);
        final RegistryInfo destRegistryInfo = registry.getRegistryInfo(destInfoFileName);
        if (!srcRegistryInfo.exists()) {
            throw new IOException("Registry info file " + srcInfoFileName + " does not exist for bundle " + srcPath);
        }
        if (destRegistryInfo.exists()) {
            throw new IOException("Destination info file " + destInfoFileName + " already exists for bundle " + destPath);
        }
        final BundleType bundleType = srcRegistryInfo.getBundleType();
        final Collection<String> locales = new HashSet<>(this.locales);
        locales.add("en");
        final Map<ResourceBundle, File> bundles = new HashMap<>();
        for (final String locale : locales) {
            final String srcBundleFileName = mapRegistryFileToResourceBundleFile(srcInfoFileName, bundleType, locale);
            final Iterator<ResourceBundle> srcBundles = registry.getAllResourceBundles(locale, srcRegistryInfo).iterator();
            if (!srcBundles.hasNext()) {
                log.info("No bundle file: {}", srcBundleFileName);
            } else {
                final String destBundleFileName = mapRegistryFileToResourceBundleFile(destInfoFileName, bundleType, locale);
                final File destBundleFile = registry.getRegistryFile(destBundleFileName);
                if (destBundleFile.exists()) {
                    throw new IOException("Destination bundle file already exists: " + destBundleFileName);
                }
                bundles.put(srcBundles.next(), destBundleFile);
            }
        }
        
        // move registry info
        destRegistryInfo.setBundleType(srcRegistryInfo.getBundleType());
        for (String key : srcRegistryInfo.getKeys()) {
            destRegistryInfo.putKeyData(key, srcRegistryInfo.getKeyData(key));
        }
        destRegistryInfo.save();
        srcRegistryInfo.delete();
        
        // move bundles
        for (Map.Entry<ResourceBundle, File> entry : bundles.entrySet()) {
            final ResourceBundle srcBundle = entry.getKey();
            srcBundle.setModuleName(moduleName);
            srcBundle.move(entry.getValue());
        }
    }

    void moveKey(final String srcPath, String srcKey, String destKey) throws IOException {
        final String srcInfoFileName = mapResourceBundleToRegistryInfoFile(srcPath);
        final RegistryInfo registryInfo = registry.getRegistryInfo(srcInfoFileName);
        if (!registryInfo.exists()) {
            throw new IOException("Registry info file " + srcInfoFileName + " does not exist for bundle " + srcPath);
        }
        final String srcRegistryKey = srcKey;
        final String destRegistryKey = destKey;
        final String bundleName = srcKey.indexOf('/') != -1 ? substringBefore(srcKey, "/") : EMPTY;
        srcKey = srcKey.indexOf('/') == -1 ? srcKey : srcKey.substring(srcKey.indexOf('/')+1);
        destKey = destKey.indexOf('/') == -1 ? destKey : destKey.substring(destKey.indexOf('/')+1);
        final KeyData keyData = registryInfo.getKeyData(srcRegistryKey);
        if (keyData == null) {
            throw new IOException("Key not found: " + srcRegistryKey);
        }
        final ResourceBundle referenceBundle = registry.getResourceBundle(bundleName, "en", registryInfo);
        if (!referenceBundle.exists()) {
            throw new IOException("Resource bundle does not exist: " + referenceBundle.getId());
        }
        if (referenceBundle.getEntries().containsKey(destKey)) {
            throw new IOException("Destination key already exists: " + destKey);
        }
        final Collection<String> locales = new HashSet<>(this.locales);
        locales.add("en");
        final Collection<ResourceBundle> bundles = new ArrayList<>();
        for (final String locale : locales) {
            final ResourceBundle resourceBundle = registry.getResourceBundle(bundleName, locale, registryInfo);
            resourceBundle.setModuleName(moduleName);
            if (!resourceBundle.exists()) {
                log.info("Bundle not found: {}", resourceBundle.getId());
            } else {
                bundles.add(resourceBundle);
            }
        }
        
        // rename key in registry
        registryInfo.putKeyData(destRegistryKey, keyData);
        registryInfo.removeKeyData(srcRegistryKey);
        registryInfo.save();
        
        // rename key in bundles
        for (final ResourceBundle bundle : bundles) {
            final Map<String, String> entries = bundle.getEntries();
            final String value = entries.get(srcKey);
            if (value != null) {
                entries.remove(srcKey);
                entries.put(destKey, value);
                bundle.save();
            }
        }
    }
    
}