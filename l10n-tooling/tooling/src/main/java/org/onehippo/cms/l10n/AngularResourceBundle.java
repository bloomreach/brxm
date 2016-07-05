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
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import net.sf.json.JSONObject;

public class AngularResourceBundle extends ResourceBundle {

    AngularResourceBundle(final String name, final String fileName, final File file) {
        super(name, fileName, file);
    }

    AngularResourceBundle(final String name, final String fileName, final String locale, final Map<String, String> entries) {
        super(name, fileName, locale, entries);
    }

    @Override
    public BundleType getType() {
        return BundleType.ANGULAR;
    }

    @Override
    protected Serializer getSerializer() {
        return new AngularBundleSerializer();
    }
    
    private class AngularBundleSerializer extends Serializer {

        @Override
        protected void serialize() throws IOException {
            createFileIfNotExists(file);
            final JSONObject o = new JSONObject();
            o.putAll(getEntries());
            FileUtils.write(file, o.toString(2), StandardCharsets.UTF_8);
        }

        @Override
        protected void deserialize() throws IOException {
            final String jsonString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            final JSONObject jsonObject = JSONObject.fromObject(jsonString);
            for (Object key : jsonObject.keySet()) {
                entries.put(key.toString(), jsonObject.get(key).toString());
            }
        }
    }

}
