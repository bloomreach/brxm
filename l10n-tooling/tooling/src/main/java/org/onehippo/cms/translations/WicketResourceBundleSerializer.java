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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

class WicketResourceBundleSerializer extends ResourceBundleSerializer {

    WicketResourceBundleSerializer(final File baseDir) {
        super(baseDir);
    }

    @Override
    public void serializeBundle(final ResourceBundle bundle) throws IOException {
        File bundleFile = getOrCreateFile(bundle.getFileName());
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : bundle.getEntries().entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        try (FileWriter writer = new FileWriter(bundleFile)) {
            properties.store(writer, null);
        }
    }

    @Override
    public ResourceBundle deserializeBundle(final String fileName, final String name, final String locale) throws IOException {
        Properties properties = new Properties();
        File file = new File(getBaseDir(), fileName);
        try (FileReader reader = new FileReader(file)) {
            properties.load(reader);
        }

        return new ResourceBundle(name, fileName, null, locale, properties) {
            @Override
            public BundleType getType() {
                return BundleType.WICKET;
            }
        };
    }

}
