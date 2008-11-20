/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;

import org.hippoecm.tools.importer.api.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File resource for the content importers.
 */
public class FileContent implements Content {

    final static Logger log = LoggerFactory.getLogger(FileContent.class);

    private File file;
    private String base;

    public FileContent(File file, String base) {
        this.file = file;
        this.base = base;
    }

    public String getName() {
        return file.getName();
    }

    public String getLocation() {
        return file.getAbsolutePath().substring(base.length());
    }

    public String getMimeType() {
        FileDataSource ds = new FileDataSource(file);
        ds.setFileTypeMap(new MimetypesFileTypeMap(getClass().getResourceAsStream("mime.types")));

        return ds.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return new BufferedInputStream(fis);
    }

    public long lastModified() {
        return file.lastModified();
    }

    public String getNodeType() {
        return "hippo:document";
    }

    public boolean isFolder() {
        return false;
    }

    public Iterator<Content> getChildren() {
        return new Iterator<Content>() {

            public boolean hasNext() {
                return false;
            }

            public Content next() {
                return null;
            }

            public void remove() {
            }

        };
    }

    protected File getFile() {
        return file;
    }

    protected String getBase() {
        return base;
    }

}
