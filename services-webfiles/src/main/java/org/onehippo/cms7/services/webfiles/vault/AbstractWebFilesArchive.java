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

import java.io.IOException;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.SubArchive;
import org.apache.jackrabbit.vault.util.Text;

public abstract class AbstractWebFilesArchive implements Archive {

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

}
