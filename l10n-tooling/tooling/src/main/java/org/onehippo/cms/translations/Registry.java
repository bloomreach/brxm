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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Registry {

    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private static final String SUFFIX = "registry.json";

    private final File registryDir;

    public Registry(File registryDir) {
        this.registryDir = registryDir;
        log.info("Opening registry in '{}'", registryDir.getPath());
    }

    public Iterable<RegistryFile> listRegistryFiles() {
        return new Iterable<RegistryFile>() {

            private Collection<File> files = loadFiles(registryDir, new ArrayList<>());

            private Collection<File> loadFiles(File directory, Collection<File> files) {
                for (File file : directory.listFiles()) {
                    if (file.isDirectory()) {
                        loadFiles(file, files);
                    } else {
                        if (file.getName().endsWith(SUFFIX)) {
                            files.add(file);
                        }
                    }
                }
                return files;
            }

            @Override
            public Iterator<RegistryFile> iterator() {
                return new Iterator<RegistryFile>() {
                    private final Iterator<File> iter = files.iterator();
                    private RegistryFile next;
                    @Override
                    public boolean hasNext() {
                        if (next == null) {
                            fetchNext();
                        }
                        return next != null;
                    }

                    @Override
                    public RegistryFile next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        RegistryFile current = next;
                        next = null;
                        return current;
                    }

                    private void fetchNext() {
                        while (next == null && iter.hasNext()) {
                            final File file = iter.next();
                            final String id = file.getAbsolutePath().substring(registryDir.getAbsolutePath().length()+1);
                            this.next = new RegistryFile(id, file);
                        }
                    }
                };
            }
        };
    }

    String mapResourceBundleToRegistryFile(final ResourceBundle resourceBundle) {
        switch (resourceBundle.getType()) {
            case ANGULAR:
                return StringUtils.substringBeforeLast(resourceBundle.getFileName(), "/")
                        + "/" + SUFFIX;
            case REPOSITORY: {
                String baseName = StringUtils.substringBefore(resourceBundle.getFileName(), ".json");
                baseName = StringUtils.substringBeforeLast(baseName, "_");
                return baseName + "." + SUFFIX;
            }
            case WICKET: {
                String baseName = StringUtils.substringBefore(resourceBundle.getFileName(), ".properties");
                baseName = StringUtils.substringBeforeLast(baseName, "_");
                return baseName + "." + SUFFIX;
            }
        }
        throw new IllegalStateException("Unknown bundle type: " + resourceBundle.getType());
    }
    
    String mapRegistryFileToResourceBundleFile(String registryFileName, BundleType bundleType, String locale) {
        switch (bundleType) {
            case ANGULAR: {
                return StringUtils.substringBefore(registryFileName, SUFFIX) 
                        + locale + ".json";
                
            }
            case REPOSITORY: {
                return StringUtils.substringBefore(registryFileName, "." + SUFFIX) 
                        + "_" + locale + ".json";
            }
            case WICKET: {
                return StringUtils.substringBefore(registryFileName, "." + SUFFIX)
                        + "_" + locale + ".properties";
            }
        }
        throw new IllegalStateException("Unknown bundle type: " + bundleType);
    }
    
    public RegistryFile loadRegistryFile(final ResourceBundle resourceBundle) throws IOException {
        return loadRegistryFile(mapResourceBundleToRegistryFile(resourceBundle));

    }

    public RegistryFile loadRegistryFile(final String fileName) throws IOException {
        final RegistryFile registryFile = new RegistryFile(fileName, new File(registryDir, fileName));
        registryFile.load();
        return registryFile;
    }
    
    public ResourceBundle loadResourceBundle(final String bundleName, final String locale, final RegistryFile registryFile) throws IOException {
        final BundleType bundleType = registryFile.getBundleType();
        if (bundleType == null) {
            log.error("Cannot load bundle for registry file {}: unknown bundle type");
        }
        final ResourceBundleSerializer serializer = ResourceBundleSerializer.create(registryDir, bundleType);
        final String bundleFileName = mapRegistryFileToResourceBundleFile(registryFile.getId(), bundleType, locale);
        return serializer.deserializeBundle(bundleFileName, bundleName, locale);
    }
    
}
