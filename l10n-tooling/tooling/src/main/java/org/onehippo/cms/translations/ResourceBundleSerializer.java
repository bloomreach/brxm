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

import java.io.File;
import java.io.IOException;

abstract class ResourceBundleSerializer {
    
    private final File baseDir;
    
    ResourceBundleSerializer(File baseDir) {
        this.baseDir = baseDir;
    }

    public static ResourceBundleSerializer create(File baseDir, BundleType type) {
        switch (type) {
            case WICKET: return new WicketResourceBundleSerializer(baseDir);
            case ANGULAR: return new AngularResourceBundleSerializer(baseDir);
            case REPOSITORY: return new RepositoryResourceBundleSerializer(baseDir);
        }
        throw new IllegalStateException("Unknown bundle type: " + type);
    }

    protected File getBaseDir() {
        return baseDir;
    }

    abstract void serializeBundle(ResourceBundle bundle) throws IOException;

    abstract ResourceBundle deserializeBundle(String fileName, String name, String locale) throws IOException;
    
    final File getOrCreateFile(String relFilePath) {
        File file = null;
        File currentDir = baseDir;
        final String[] elements = relFilePath.split("/");
        for (int i = 0; i < elements.length; i++) {
            if (i == elements.length - 1) {
                file = new File(currentDir, elements[i]);
            } else {
                currentDir = new File(currentDir, elements[i]);
                if (!currentDir.exists()) {
                    if (!currentDir.mkdir()) {
                        throw new IllegalStateException("Cannot create directory " + currentDir.getPath());
                    }
                }
            }
        }
        return file;
    }
    
}
