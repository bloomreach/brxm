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
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Registry {

    private static final Logger log = LoggerFactory.getLogger(Registry.class);

    private final File registryDir;

    public Registry(File registryDir) {
        this.registryDir = registryDir;
        log.debug("Opening registry in '{}'", registryDir.getPath());
    }
    
    public File getRegistryFile(final String relPath) {
        return new File(registryDir, relPath);
    }
    
    public Iterable<RegistryInfo> getRegistryInfos() {
        return new Iterable<RegistryInfo>() {

            private Collection<File> files = loadFiles(registryDir, new ArrayList<>());

            private Collection<File> loadFiles(File directory, Collection<File> files) {
                final File[] listFiles = directory.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (file.isDirectory()) {
                            loadFiles(file, files);
                        } else {
                            if (file.getName().endsWith(TranslationsUtils.REGISTRY_FILE_SUFFIX)) {
                                files.add(file);
                            }
                        }
                    }
                }
                return files;
            }

            @Override
            public Iterator<RegistryInfo> iterator() {
                return new Iterator<RegistryInfo>() {
                    private final Iterator<File> iter = files.iterator();
                    private RegistryInfo next;
                    @Override
                    public boolean hasNext() {
                        if (next == null) {
                            fetchNext();
                        }
                        return next != null;
                    }

                    @Override
                    public RegistryInfo next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        final RegistryInfo current = next;
                        next = null;
                        return current;
                    }

                    private void fetchNext() {
                        while (next == null && iter.hasNext()) {
                            try {
                                final File file = iter.next();
                                final String infoFileName = file.toURI().toString().substring(registryDir.toURI().toString().length()+1);
                                this.next = new RegistryInfo(infoFileName, file);
                                this.next.load();
                            } catch (IOException e) {
                                log.error("Failed to load registry info");
                                next = null;
                            }
                        }
                    }
                };
            }
        };
    }

    public RegistryInfo getRegistryInfoForBundle(final ResourceBundle resourceBundle) throws IOException {
        return getRegistryInfo(TranslationsUtils.mapResourceBundleToRegistryInfoFile(resourceBundle));
    }

    public RegistryInfo getRegistryInfo(final String infoFileName) throws IOException {
        final RegistryInfo registryInfo = new RegistryInfo(infoFileName, new File(registryDir, infoFileName));
        registryInfo.load();
        return registryInfo;
    }
    
    public Iterable<ResourceBundle> getReferenceBundles() {
        return new Iterable<ResourceBundle>() {
            
            @Override
            public Iterator<ResourceBundle> iterator() {
                return new Iterator<ResourceBundle>() {
                    private final Iterator<RegistryInfo> registryInfos = getRegistryInfos().iterator();
                    private Iterator<ResourceBundle> nextBundles;
                    private ResourceBundle next;
                    
                    @Override
                    public boolean hasNext() {
                        if (next == null) {
                            fetchNext();
                        }
                        return next != null;
                    }

                    @Override
                    public ResourceBundle next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        final ResourceBundle current = next;
                        next = null;
                        return current;
                    }
                    
                    private void fetchNext() {
                        while ((nextBundles == null || !nextBundles.hasNext()) && registryInfos.hasNext()) {
                            final RegistryInfo nextInfo = registryInfos.next();
                            try {
                                nextBundles = getAllResourceBundles("en", nextInfo).iterator();
                            } catch (IOException e) {
                                log.error("Failed to fetch reference resource bundle of info {}", nextInfo.getFileName());
                            }
                        }
                        if (nextBundles != null && nextBundles.hasNext()) {
                            next = nextBundles.next();
                            try {
                                next.load();
                            } catch (IOException e) {
                                log.error("Failed to load bundle: {}", next.getFileName());
                                fetchNext();
                            }
                        }
                    }
                };
            }
        };
    }
    
    public ResourceBundle getResourceBundle(final String bundleName, final String bundleFileName, final BundleType bundleType) throws IOException {
        final ResourceBundle resourceBundle = ResourceBundle.createInstance(bundleName, bundleFileName, new File(registryDir, bundleFileName), bundleType);
        resourceBundle.load();
        return resourceBundle;
    }
    
    public ResourceBundle getResourceBundle(final String bundleName, final String locale, final RegistryInfo registryInfo) throws IOException {
        final BundleType bundleType = registryInfo.getBundleType();
        final String bundleFileName = TranslationsUtils.mapRegistryFileToResourceBundleFile(registryInfo.getFileName(), bundleType, locale);
        return getResourceBundle(bundleName, bundleFileName, bundleType);
    }
    
    public Iterable<ResourceBundle> getAllResourceBundles(final String locale, final RegistryInfo registryInfo) throws IOException {
        final BundleType bundleType = registryInfo.getBundleType();
        final String bundleFileName = TranslationsUtils.mapRegistryFileToResourceBundleFile(registryInfo.getFileName(), bundleType, locale);
        return ResourceBundle.createAllInstances(bundleFileName, new File(registryDir, bundleFileName), registryInfo.getBundleType(), locale);
    }
    
}
