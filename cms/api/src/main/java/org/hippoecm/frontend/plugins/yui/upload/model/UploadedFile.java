/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.model;

import java.io.File;

import org.apache.commons.fileupload.FileItem;

/**
 * This class represents the file that is being uploaded. It contains the file and some extra information. This object
 * is initialized with the data from the original object being uploaded and can be transformed during the execution of
 * {@link IUploadPreProcessor#process(UploadedFile)}, so the data can be transformed before being persisted.
 */
public class UploadedFile {

    private final String fieldName;
    private final boolean isFormField;
    private String contentType;
    private String fileName;
    private File file;

    public UploadedFile(final File file, final FileItem fileItem) {
        this.file = file;
        this.fieldName = fileItem.getFieldName();
        this.contentType = fileItem.getContentType();
        this.isFormField = fileItem.isFormField();
        this.fileName = fileItem.getName();
    }

    public File getFile() {
        return file;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public boolean isFormField() {
        return isFormField;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
