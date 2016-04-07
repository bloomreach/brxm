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
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Extractor {

    private static Logger log = LoggerFactory.getLogger(Extractor.class);

    private final File baseDir;
    private final Collection<String> locales;
    
    public Extractor(final File baseDir, final Collection<String> locales) {
        this.baseDir = baseDir;
        this.locales = locales;
    }
    
    public void extract() throws IOException {
        for (ResourceBundleLoader loader : ResourceBundleLoader.getResourceBundleLoaders(locales)) {
            for (ResourceBundle resourceBundle : loader.loadBundles()) {
                final ResourceBundleSerializer resourceBundleSerializer = 
                        ResourceBundleSerializer.create(baseDir, resourceBundle.getType());
                resourceBundleSerializer.serializeBundle(resourceBundle);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        File baseDir = new File(args[0]);
        if (!baseDir.exists()) {
            throw new IllegalStateException("Directory does no exist: " + baseDir.getPath());
        }
        final Collection<String> locales = Arrays.asList(args[1].split(","));
        new Extractor(baseDir, locales).extract();
    }
    
}
