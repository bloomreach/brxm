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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;

class RepositoryResourceBundleSerializer extends ResourceBundleSerializer {
    
    protected RepositoryResourceBundleSerializer(final File baseDir) {
        super(baseDir);
    }

    @Override
    public void serializeBundle(final ResourceBundle bundle) throws IOException {
        final File bundlesFile = getOrCreateFile(bundle.getFileName());
        final JSONObject o;
        if (bundlesFile.exists()) {
            final String json = FileUtils.readFileToString(bundlesFile);
            o = JSONObject.fromObject(json);
        } else {
            o = new JSONObject();
        }
        JSONObject current = o;
        final String[] bundles = bundle.getName().split("\\.");
        for (String name : bundles) {
            if (!current.has(name)) {
                current.put(name, new JSONObject());
            }
            current = (JSONObject) current.get(name);
        }
        if (!current.has(bundle.getLocale())) {
            current.put(bundle.getLocale(), new JSONObject());
        }
        current = (JSONObject) current.get(bundle.getLocale());
        current.putAll(bundle.getEntries());
        FileUtils.write(bundlesFile, o.toString(2));
        final File extensionFile = getExtensionFile(bundle);
        final RepositoryExtension extension;
        if (extensionFile.exists()) {
            extension = RepositoryExtension.load(extensionFile);
        } else {
            extension = RepositoryExtension.create();
        }
        if (!extension.containsResourceBundle(bundle)) {
            extension.addResourceBundlesItem(bundle);
            extension.save(extensionFile);
        }
    }

    @Override
    public ResourceBundle deserializeBundle(final String fileName, final String name, final String locale) throws IOException {
        File file = new File(getBaseDir(), fileName);
        if (!file.exists()) {
            return null;
        }

        final String json = FileUtils.readFileToString(file);
        JSONObject obj = JSONObject.fromObject(json);

        // navigate to the right bundle in the json file
        String[] bundles = name.split("\\.");
        for (String bundle : bundles) {
            if (!obj.has(bundle)) {
                return null;
            }
            obj = (JSONObject) obj.get(bundle);
        }

        // navigate to the right locale
        if (!obj.has(locale)) {
            return null;
        }
        obj = (JSONObject) obj.get(locale);

        Properties properties = new Properties();
        for (Object key : obj.keySet()) {
            Object value = obj.get(key);
            if (value instanceof String) {
                properties.setProperty(key.toString(), value.toString());
            }
        }

        return new RepositoryResourceBundle(name, fileName, null, locale, properties);
    }
    
    private File getExtensionFile(final ResourceBundle bundle) {
        return getOrCreateFile("extensions/" + bundle.getLocale() + "/hippoecm-extension.xml");
    }

}
