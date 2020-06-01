/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.webfiles;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;

import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;


/**
 * Serializable web file that is stored in memory and can be cached.
 */
public class CacheableWebFile implements WebFile, Serializable {

    private final String path;
    private final String name;
    private final String encoding;
    private final Calendar lastModified;
    private final String mimeType;
    private final Binary binary;
    private final String version;

    public CacheableWebFile(final WebFile resource, final String version) throws IOException {
        path = resource.getPath();
        name = resource.getName();
        encoding = resource.getEncoding();
        lastModified = resource.getLastModified();
        mimeType = resource.getMimeType();
        binary = new CacheableBinary(resource.getBinary());
        this.version = version;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public Calendar getLastModified() {
        return lastModified;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Binary getBinary() {
        return binary;
    }

    public String getVersion() {
        return version;
    }
}
