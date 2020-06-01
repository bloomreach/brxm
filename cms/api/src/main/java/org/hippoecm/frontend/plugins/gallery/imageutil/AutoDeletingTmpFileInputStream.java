/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AutoDeletingTmpFileInputStream extends FileInputStream {

    public static final Logger log = LoggerFactory.getLogger(AutoDeletingTmpFileInputStream.class);

    private final File tmpFile;

    AutoDeletingTmpFileInputStream(final File tmpFile) throws FileNotFoundException {
        super(tmpFile);
        this.tmpFile = tmpFile;
    }

    @Override
    public void close() throws IOException {
        super.close();
        log.debug("Deleting temporary file {}", tmpFile);
        tmpFile.delete();
    }

}
