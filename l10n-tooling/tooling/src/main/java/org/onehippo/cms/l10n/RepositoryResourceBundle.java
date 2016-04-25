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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;
import static org.apache.commons.lang.StringUtils.substringBefore;

public class RepositoryResourceBundle extends ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(RepositoryResourceBundle.class);

    RepositoryResourceBundle(final String name, final String fileName, final File file) {
        super(name, fileName, file);
    }

    RepositoryResourceBundle(final String name, final String fileName, final String locale, final Map<String, String> entries) {
        super(name, fileName, locale, entries);
    }

    @Override
    public String getId() {
        return getFileName() + "/" + getName();
    }

    @Override
    public BundleType getType() {
        return BundleType.REPOSITORY;
    }

    @Override
    protected RepositoryBundleSerializer getSerializer() {
        return new RepositoryBundleSerializer();
    }

    private File getBaseDir() {
        return new File(substringBefore(file.getAbsolutePath(), fileName));
    }
    
    File getExtensionFile() {
        return new File(getBaseDir(), getExtensionFileName());
    }

    private String getExtensionFileName() {
        return "extensions/" + locale + "/hippoecm-extension.xml";
    }

    @Override
    public boolean exists() {
        if (!file.exists()) {
            return false;
        }
        try {
            return getSerializer().exists();
        } catch (IOException e) {
            log.error("Failed to determine whether bundle exists", e);
            return false;
        }
    }

    @Override
    public void delete() throws IOException {
        getSerializer().delete();
    }

    @Override
    public void move(final File destFile) throws IOException {
        final File extensionFile = getExtensionFile();
        if (!extensionFile.exists()) {
            throw new IOException("Extension file not found: " + getExtensionFileName());
        }
        final RepositoryExtension extension = RepositoryExtension.load(extensionFile);
        extension.removeResourceBundlesItem(this);
        super.move(destFile);
        extension.addResourceBundlesItem(this);
        extension.save(extensionFile);
    }

    static Iterable<ResourceBundle> createAllInstances(final String fileName, final File file, final String locale) throws IOException {
        final Collection<ResourceBundle> bundles = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(file)) {
            final String json = IOUtils.toString(in);
            final JSONObject jsonObject = JSONObject.fromObject(json);
            RepositoryResourceBundleLoader.collectResourceBundles(jsonObject, fileName, Collections.singletonList(locale), new RepositoryResourceBundleLoader.Path(), bundles);
        }
        for (ResourceBundle bundle : bundles) {
            ((RepositoryResourceBundle) bundle).file = file;
        }
        return bundles;
    }
    
    private class RepositoryBundleSerializer extends Serializer {

        @Override
        protected void serialize() throws IOException {
            final JSONObject o;
            if (file.exists()) {
                final String json = FileUtils.readFileToString(file);
                o = JSONObject.fromObject(json);
            } else {
                o = new JSONObject();
            }
            JSONObject current = o;
            final String[] bundles = name.split("\\.");
            for (String name : bundles) {
                if (!current.has(name)) {
                    current.put(name, new JSONObject());
                }
                current = (JSONObject) current.get(name);
            }
            if (!current.has(locale)) {
                current.put(locale, new JSONObject());
            }
            current = (JSONObject) current.get(locale);
            current.clear();
            current.putAll(entries);
            FileUtils.write(file, o.toString(2));
            final File extensionFile = getExtensionFile();
            final RepositoryExtension extension;
            if (extensionFile.exists()) {
                extension = RepositoryExtension.load(extensionFile);
            } else {
                extension = RepositoryExtension.create();
            }
            if (!extension.containsResourceBundle(RepositoryResourceBundle.this)) {
                extension.addResourceBundlesItem(RepositoryResourceBundle.this);
                log.debug("Saving extension file {}", getExtensionFileName());
                createFileIfNotExists(extensionFile);
                extension.save(extensionFile);
            }

        }

        @Override
        protected void deserialize() throws IOException {
            final String json = FileUtils.readFileToString(file);
            JSONObject obj = JSONObject.fromObject(json);

            // navigate to the right bundle in the json file
            String[] bundles = name.split("\\.");
            for (String bundle : bundles) {
                if (!obj.has(bundle)) {
                    return;
                }
                obj = (JSONObject) obj.get(bundle);
            }

            // navigate to the right locale
            if (!obj.has(locale)) {
                return;
            }
            obj = (JSONObject) obj.get(locale);
            
            for (Object key : obj.keySet()) {
                Object value = obj.get(key);
                if (value instanceof String) {
                    entries.put(key.toString(), value.toString());
                }
            }
        }
        
        private void delete() throws IOException {
            if (!file.exists()) {
                return;
            }
            final String json = FileUtils.readFileToString(file);
            final Stack<JSONObject> stack = new Stack<>();
            final JSONObject o = JSONObject.fromObject(json);
            JSONObject current = o;
            stack.push(current);
            final String[] bundles = name.split("\\.");
            for (String name : bundles) {
                if (!current.has(name)) {
                    current.put(name, new JSONObject());
                }
                current = (JSONObject) current.get(name);
                stack.push(current);
            }
            current.remove(locale);
            stack.pop();
            for (int i = bundles.length-1; i > -1 && current.isEmpty(); i--) {
                current = stack.pop();
                log.debug("Deleting bundle {}", bundles[i]);
                current.remove(bundles[i]);
            }
            if (o.isEmpty()) {
                log.debug("Deleting file {}", fileName);
                file.delete();
                final File extensionFile = getExtensionFile();
                if (extensionFile.exists()) {
                    final RepositoryExtension extension = RepositoryExtension.load(extensionFile);
                    extension.removeResourceBundlesItem(RepositoryResourceBundle.this);
                    if (extension.isEmpty()) {
                        extensionFile.delete();
                    } else {
                        extension.save(extensionFile);
                    }
                } 
            } else {
                FileUtils.write(file, o.toString(2));
            }
        }
        
        private boolean exists() throws IOException {
            final String json = FileUtils.readFileToString(file);
            JSONObject obj = JSONObject.fromObject(json);

            // navigate to the right bundle in the json file
            String[] bundles = name.split("\\.");
            for (String bundle : bundles) {
                if (!obj.has(bundle)) {
                    return false;
                }
                obj = (JSONObject) obj.get(bundle);
            }

            // navigate to the right locale
            if (!obj.has(locale)) {
                return false;
            }
            return true;
        }
    }
}
