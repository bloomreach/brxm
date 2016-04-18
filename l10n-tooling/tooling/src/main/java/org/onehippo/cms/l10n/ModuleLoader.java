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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

public class ModuleLoader {

    private final File baseDir;
    
    public ModuleLoader(final File baseDir) {
        this.baseDir =  baseDir;
    }
    
    public Collection<Module> loadModules() {
        final Collection<Module> modules = new ArrayList<>();
        for (File pom : collectPoms(baseDir, new ArrayList<>())) {
            final Module module = new Module(pom);
            if (module.hasResources()) {
                modules.add(module);
            }
        }
        return modules;
    }

    private Collection<File> collectPoms(File baseDir, Collection<File> poms) {
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                if (file.getName().equals("resources") || file.getName().equals("target")) {
                    return false;
                }
                return true;
            }
        };
        for (File file : baseDir.listFiles(filter)) {
            if (file.isDirectory()) {
                collectPoms(file, poms);
            } else if (file.getName().equals("pom.xml")) {
                poms.add(file);
            }
        }
        return poms;
    }

}
