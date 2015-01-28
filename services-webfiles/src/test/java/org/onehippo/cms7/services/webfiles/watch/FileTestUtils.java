/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.watch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTestUtils {

    private static final Logger log = LoggerFactory.getLogger(FileTestUtils.class);

    private FileTestUtils() {
    }

    /**
     * Improved version of {@link org.apache.commons.io.FileUtils#touch(File)}. It ensures a unique last modified
     * timestamp after each call. It also does not set the last modified time twice when creating a new file.
     * Note that while last modified time is set in milliseconds, most platforms actually round it down to seconds.
     */
    public static void forceTouch(final File file) throws IOException {
        if (!file.exists()) {
            OutputStream out = FileUtils.openOutputStream(file);
            IOUtils.closeQuietly(out);
        } else {
            final long lastModified = file.lastModified();
            final long nextSecond = lastModified + 1000;
            long newModified = System.currentTimeMillis();
            if (newModified < nextSecond) {
                newModified = nextSecond;
            }
            log.debug("Touched {} at {}", file, newModified);
            boolean success = file.setLastModified(newModified);
            if (!success) {
                throw new IOException("Unable to set the last modification time for " + file);
            }
        }
    }

}
