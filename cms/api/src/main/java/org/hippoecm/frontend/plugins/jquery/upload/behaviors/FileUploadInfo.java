/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.behaviors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to store file information prior uploading
 */
public class FileUploadInfo {
    private final String fileName;
    private final long size;
    private final List<String> errorMessages = new ArrayList<>();

    public FileUploadInfo(final String fileName, final long size) {
        this.fileName = fileName;
        this.size = size;
    }

    public void addErrorMessage(final String message) {
        this.errorMessages.add(message);
    }

    /**
     * Return either an empty list or a list of error messages occurred in uploading
     * @return
     */
    public List<String> getErrorMessages() {
        return Collections.unmodifiableList(errorMessages);
    }

    public long getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }
}
