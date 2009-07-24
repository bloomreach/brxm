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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.hippoecm.tools.importer.api.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File resource for the content importers.
 */
public class FolderContent extends FileContent {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(FolderContent.class);

    public FolderContent(File file, String base) {
        super(file, base);
    }

    public String getMimeType() {
        return null;
    }

    public InputStream getInputStream() throws IOException {
        return null;
    }

    public boolean isFolder() {
        return true;
    }

    public Iterator<Content> getChildren() {
        final File[] files = getFile().listFiles();
        if (files == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        
        final Iterator<File> base = new ArrayIterator(files);
        
        return new Iterator<Content>() {
            private Content next;
            private boolean ready = false;

            public boolean hasNext() {
                load();
                return (next != null);
            }

            public Content next() {
                load();
                ready = false;
                return next;
            }

            void load() {
                if (!ready) {
                    next = null;
                    ready = true;
                    while (base.hasNext()) {
                        File child = (File) base.next();
                        if (child.getName().startsWith(".")) {
                            continue;
                        }
                        if (child.isDirectory()) {
                            next = new FolderContent(child, getImportBase());
                            break;
                        } else {
                            next = new FileContent(child, getImportBase());
                            break;
                        }
                    }
                }
            }

            public void remove() {
                base.remove();
            }
        };
    }

}
