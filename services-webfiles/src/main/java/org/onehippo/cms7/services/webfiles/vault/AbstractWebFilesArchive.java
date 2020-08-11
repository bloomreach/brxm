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
package org.onehippo.cms7.services.webfiles.vault;

import java.io.File;
import java.io.IOException;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.SubArchive;
import org.apache.jackrabbit.vault.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWebFilesArchive implements Archive {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebFilesArchive.class);
    public Archive.Entry getEntry(String path) throws IOException {
        String[] segs = Text.explode(path, '/');
        Archive.Entry root = getRoot();
        for (String name: segs) {
            root = root.getChild(name);
            if (root == null) {
                break;
            }
        }
        return root;
    }

    public Archive getSubArchive(String rootPath, boolean asJcrRoot) throws IOException {
        Archive.Entry root = getEntry(rootPath);
        return root == null ? null : new SubArchive(this, root, asJcrRoot);
    }

    /**
     * @return the root directory in this archive that contains all web files.
     */
    public abstract Entry getBundleRoot();


    public static void logSizeExceededWarning(final File file, final long maxFileLengthBytes) {
        log.warn("Skipping file '{}' of {} KB ({} bytes) because it exceeds the maximum allowed file length of {} KB ({} bytes). If larger " +
                        "files need to be supported, increase '{}'",
                file, file.length()/1024, file.length(), maxFileLengthBytes/1024, maxFileLengthBytes,
                "/hippo:configuration/hippo:modules/webfiles/hippo:moduleconfig/@maxFileLengthKb");
    }

    /**
     * If the raw file name matches the "_prefix_nodename" pattern, we need to simulate an extra
     * leading underscore to avoid FileVault mangling, like so "__prefix_nodename".
     * NOTE: we don't currently export webfiles again, so there's no current need to reverse this.
     * @param rawName unmangled name that we want to keep unmangled
     * @return rawName or rawName with an extra leading underscore to defeat FileVault's default namespace mangling
     */
    public static String defeatNamespaceMangling(final String rawName) {
        if (rawName.matches("_(.+)_(.+)")) {
            return "_"+rawName;
        }
        else {
            return rawName;
        }
    }

}
