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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class ResourceBundle {

    private final String name;
    private String fileName;
    private final ArtifactInfo artifactInfo;
    private final String locale;
    private final Map<String, String> entries;

    public ResourceBundle(final String name, final String fileName, final ArtifactInfo artifactInfo, final String locale, final Properties properties) {
        this.name = name;
        this.fileName = fileName;
        this.artifactInfo = artifactInfo;
        this.locale = locale;
        entries = new HashMap<>();
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                entries.put(key, properties.getProperty(key));
            }
        }
    }
    
    public String getId() {
        return artifactInfo.getGroupId() + "/" + artifactInfo.getArtifactId() + "/" + artifactInfo.getVersion() + "/" + fileName;
    }
    
    public String getName() {
        return name;
    }

    public abstract BundleType getType();

    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
    
    public ArtifactInfo getArtifactInfo() {
        return artifactInfo;
    }

    public String getLocale() {
        return locale;
    }
    
    public Map<String, String> getEntries() {
        return entries;
    }

}
