/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation.components.document;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.util.lang.PropertyResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.JSONIdentifier;

final class FolderTranslationStore extends ExtJsonStore<FolderTranslation> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTranslationStore.class);
    
    private final List<FolderTranslation> translations;

    FolderTranslationStore(List<FolderTranslation> translations) {
        super(Arrays.asList(new ExtField("name"), new ExtField("namefr"), new ExtField("url"),
                new ExtField("urlfr"), new ExtField("id"), new ExtField("type"), new ExtField(
                        "editable", Boolean.class)));
        this.translations = translations;
    }

    @Override
    protected JSONObject updateRecord(JSONObject record) {
        try {
            String id = record.getString("id");
            for (FolderTranslation data : translations) {
                if (data.getId().equals(id)) {
                    if (record.has("namefr")) {
                        data.setNamefr(record.getString("namefr"));
                        if (record.has("urlfr") && !"".equals(record.getString("urlfr"))) {
                            data.setUrlfr(record.getString("urlfr"));
                        } else {
                            data.setUrlfr(record.getString("namefr").toLowerCase());
                        }
                    }

                    JSONObject jsonLine = new JSONObject();
                    for (ExtField field : getFields()) {
                        Object value = PropertyResolver.getValue(field.getName(), data);
                        jsonLine.put(field.getName(), value);
                    }
                    return jsonLine;
                }
            }
        } catch (JSONException e) {
            log.error("Could not update record, exception " + e.getMessage());
        }
        return null;
    }

    @Override
    protected JSONObject getProperties() {
        JSONObject properties = super.getProperties();
        try {
            properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        } catch (JSONException e) {
            log.error("Could not add writer to properties, " + e.getMessage());
        }
        return properties;
    }

    @Override
    protected JSONArray getData() {
        JSONArray jsonData = new JSONArray();
        try {
            for (FolderTranslation record : translations) {
                JSONObject jsonLine = new JSONObject();
                for (ExtField field : getFields()) {
                    Object value = PropertyResolver.getValue(field.getName(), record);
                    jsonLine.put(field.getName(), value);
                }
                jsonData.put(jsonLine);
            }
        } catch (JSONException e) {
            log.error("Failed to initialize data, " + e.getMessage());
        }
        return jsonData;
    }
}