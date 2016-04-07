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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistryFile {
    
    private static final Logger log = LoggerFactory.getLogger(RegistryFile.class);

    private final File file;
    private final String id;
    
    private RegistryData data = new RegistryData();

    public RegistryFile(final String id, final File file) {
        this.id = id;
        this.file = file;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public void load() throws IOException {
        if (!file.exists()) {
            return;
        }
        log.info("Loading registry file: " + id);
        try (final InputStream in = new FileInputStream(file)) {
            data = getObjectMapper().readValue(in, new TypeReference<RegistryData>(){});
        }
    }

    public void save() throws IOException {
        log.info("Saving registry file: " + id);
        try (final FileOutputStream out = FileUtils.openOutputStream(file)) {
            getObjectMapper().writeValue(out, data);
        }
    }
    
    public String getId() {
        return id;
    }

    public BundleType getBundleType() {
        return data.getBundleType();
    }
    
    public void setBundleType(BundleType bundleType) {
        data.setBundleType(bundleType);
    }
    
    public KeyData getKeyData(String key) {
        return data.getKeyData().get(key);
    }

    public KeyData getOrCreateKeyData(String key) {
        KeyData keyData = getKeyData(key);
        if (keyData == null) {
            keyData = new KeyData();
            putKeyData(key, keyData);
        }
        return keyData;
    }

    public KeyData putKeyData(String key, KeyData keyData) {
        return data.getKeyData().put(key, keyData);
    }

    public Set<String> getKeys() {
        return data.getKeyData().keySet();
    }
    
}
