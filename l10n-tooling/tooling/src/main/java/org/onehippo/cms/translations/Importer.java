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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Importer {
    
    private static final Logger log = LoggerFactory.getLogger(Importer.class);
    
    private final File baseDir;
    private final String format;
    
    Importer(final File baseDir, final String format) {
        this.baseDir = baseDir;
        this.format = format;
    }
    
    void _import(final String fileName, final String locale) throws IOException {
        final Map<String, Module> modules = new HashMap<>();
        for (Module module : new ModuleLoader(baseDir).loadModules()) {
            modules.put(module.getName(), module);
        }
        final File csv = new File(baseDir, fileName);
        try (final BufferedReader reader = new BufferedReader(new FileReader(csv))) {
            final CSVParser parser = new CSVParser(reader, CSVFormat.valueOf(format));
            final List<CSVRecord> records = parser.getRecords();
            for (int i = 1; i < records.size(); i++) {
                final CSVRecord record = records.get(i);
                if (record.size() != 3) {
                    throw new IllegalArgumentException("Unexpected record size: " + record.size() + " at record " + record.getRecordNumber());
                }
                final FQKey fqKey = new FQKey(record.get(0));
                final String translation = record.get(2);
                final Module module = modules.get(fqKey.moduleId);
                if (module == null) {
                    log.error("No such module: {}", fqKey.moduleId);
                    continue;
                }
                final RegistryInfo registryInfo = module.getRegistry().getRegistryInfo(fqKey.registryFile);
                final ResourceBundle resourceBundle = module.getRegistry().getResourceBundle(fqKey.bundleName, locale, registryInfo);
                resourceBundle.getEntries().put(fqKey.key, translation);
                log.info("Saving resource bundle: " + resourceBundle.getFileName());
                resourceBundle.save();
                registryInfo.getKeyData(fqKey.key).setLocaleStatus(locale, KeyData.LocaleStatus.RESOLVED);
                registryInfo.save();        
            }
        }
    }
    
    private static class FQKey {
        
        private final String moduleId;
        private final String registryFile;
        private final String key;
        private final String bundleName;
        
        private FQKey(final String fqKey) {
            moduleId = StringUtils.substringBefore(fqKey, "#");
            String rest = StringUtils.substringAfter(fqKey, "#");
            registryFile = StringUtils.substringBefore(rest, "#");
            String key = StringUtils.substringAfter(rest, "#");
            final int offset = key.indexOf('/');
            if (offset != -1) {
                bundleName = key.substring(0, offset);
                this.key = key.substring(offset+1);
            } else {
                bundleName = "";
                this.key = key;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        final String baseDir = args[0];
        final String format = args[1];
        final String locale = args[2];
        final String fileName = args[3];
        if (StringUtils.isBlank(locale)) {
            throw new IllegalArgumentException("Missing locale argument");
        }
        new Importer(new File(baseDir), format)._import(fileName, locale);
    }
    
}
