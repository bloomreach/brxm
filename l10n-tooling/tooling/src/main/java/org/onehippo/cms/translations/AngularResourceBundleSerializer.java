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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import net.sf.json.JSONObject;

class AngularResourceBundleSerializer extends ResourceBundleSerializer {

    AngularResourceBundleSerializer(final File baseDir) {
        super(baseDir);
    }

    @Override
    void serializeBundle(final ResourceBundle bundle) throws IOException {
        final JSONObject o = new JSONObject();
        o.putAll(bundle.getEntries());
        final File file = getOrCreateFile(bundle.getFileName());
        try (FileWriter writer = new FileWriter(file)) {
            o.write(writer);
        }
    }

    @Override
    ResourceBundle deserializeBundle(final String fileName, final String name, final String locale) throws IOException {
        final String jsonString = FileUtils.readFileToString(new File(getBaseDir(), fileName));
        final JSONObject jsonObject = JSONObject.fromObject(jsonString);
        final Properties properties = new Properties();

        for (Object o : jsonObject.keySet()) {
            properties.put(o, jsonObject.get(o));
        }

        return new AngularResourceBundle(name, fileName, null, locale, properties);
    }

}
