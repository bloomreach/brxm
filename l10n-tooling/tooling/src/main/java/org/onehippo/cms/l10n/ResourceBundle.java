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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.substringBefore;

public abstract class ResourceBundle {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceBundle.class);

    protected final String name;
    protected String fileName;
    protected File file;
    protected final String locale;
    protected final Map<String, String> entries;
    protected String moduleName;
    
    ResourceBundle(final String name, final String fileName, final File file) {
        this.name = name;
        this.fileName = fileName;
        this.file = file;
        this.locale = TranslationsUtils.getLocaleFromBundleFileName(fileName, getType());
        this.entries = new HashMap<>();
    }
    
    ResourceBundle(final String name, final String fileName, final String locale, final Map<String, String> entries) {
        this.name = name;
        this.fileName = fileName;
        this.locale = locale;
        this.entries = entries;
        this.file = null;
    }
    
    public String getId() {
        return fileName;
    }
    
    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }
    
    public abstract BundleType getType();
    
    public String getModuleName() {
        return moduleName;
    }
    
    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public Map<String, String> getEntries() {
        return entries;
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    public boolean exists() {
        return file.exists();
    }
    
    public void load() throws IOException {
        log.debug("Loading bundle from file {}", fileName);
        if (file == null) {
            throw new IOException("No bundle file to load");
        }
        if (file.exists()) {
            entries.clear();
            getSerializer().deserialize();
        }
    }
    
    public void save() throws IOException {
        log.debug("Saving bundle to file {}", fileName);
        if (file == null) {
            throw new IOException("No bundle file to save");
        }
        if (entries == null || entries.isEmpty()) {
            throw new IOException("No entries to save");
        }
        getSerializer().serialize();
    }
    
    public void delete() throws IOException {
        if (file.exists()) {
            file.delete();
        }
    }

    public void move(final File destFile) throws IOException {
        final File baseDir = getBaseDir();
        FileUtils.copyFile(file, destFile);
        delete();
        file = destFile;
        fileName = file.getCanonicalPath().substring(baseDir.getCanonicalPath().length()+1);
    }
    
    private File getBaseDir() throws IOException {
        return new File(substringBefore(file.getCanonicalPath(), fileName));
    }

    protected abstract Serializer getSerializer();
    
    public static ResourceBundle createInstance(final String name, final String fileName, final File file, final BundleType bundleType) {
        switch (bundleType) {
            case ANGULAR: {
                return new AngularResourceBundle(name, fileName, file);
            }
            case WICKET: {
                return new WicketResourceBundle(name, fileName, file);
            }
            case REPOSITORY: {
                return new RepositoryResourceBundle(name, fileName, file);
            }
        }
        throw new IllegalArgumentException("No such bundle type: " + bundleType);
    }
    
    public static Iterable<ResourceBundle> createAllInstances(final String fileName, final File file, final BundleType bundleType, final String locale) throws IOException {
        if (!file.exists()) {
            return Collections.emptyList();
        }
        switch (bundleType) {
            case ANGULAR: {
                return Collections.singletonList(new AngularResourceBundle(null, fileName, file));
            }
            case WICKET: {
                return Collections.singleton(new WicketResourceBundle(null, fileName, file));
            }
            case REPOSITORY: {
                return RepositoryResourceBundle.createAllInstances(fileName, file, locale);
            }
        }
        throw new IllegalArgumentException("No such bundle type: " + bundleType);
        
    }
    
    protected abstract class Serializer {
        
        protected abstract void serialize() throws IOException;
        
        protected abstract void deserialize() throws IOException;
        
        protected void createFileIfNotExists(final File file) throws IOException {
            if (!file.exists()) {
                final File directory = file.getParentFile();
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        throw new IOException("Failed to create directory for bundle file " + fileName);
                    }
                }
            }
        }
    }

}
