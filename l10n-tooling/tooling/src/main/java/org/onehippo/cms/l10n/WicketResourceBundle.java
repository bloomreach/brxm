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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class WicketResourceBundle extends ResourceBundle {

    WicketResourceBundle(final String name, final String fileName, final File file) {
        super(name, fileName, file);
    }

    WicketResourceBundle(final String name, final String fileName, final String locale, final Map<String, String> entries) {
        super(name, fileName, locale, entries);
    }

    @Override
    public BundleType getType() {
        return BundleType.WICKET;
    }

    @Override
    protected Serializer getSerializer() {
        return new WicketBundleSerializer();
    }

    private class WicketBundleSerializer extends Serializer {
        
        @Override
        public void serialize() throws IOException {
            createFileIfNotExists(file);
            Properties properties = new Properties();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
            try (FileOutputStream stream = new FileOutputStream(file)) {
                properties.store(stream, null);
            }
        }

        @Override
        public void deserialize() throws IOException {
            Properties properties = new Properties();
            try (FileInputStream stream = new FileInputStream(file)) {
                properties.load(stream);
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                entries.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }
}
