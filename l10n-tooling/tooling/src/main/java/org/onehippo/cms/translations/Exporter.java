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
import java.util.ArrayList;
import java.util.Collection;

import static org.onehippo.cms.translations.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.translations.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.UNRESOLVED;

public class Exporter {
    
    private final File baseDir;
    
    Exporter(final File baseDir) {
        this.baseDir = baseDir;
    }
    
    File export(final String locale) throws Exception {
        final Collection<Translation> pending = new ArrayList<>();
        for (Module module : new ModuleLoader(baseDir).loadModules()) {
            final Registry registry = module.getRegistry();
            for (final RegistryFile registryFile : registry.listRegistryFiles()) {
                registryFile.load();
                for (final String key : registryFile.getKeys()) {
                    final KeyData data = registryFile.getKeyData(key);
                    if (data.getStatus() == ADDED || data.getStatus() == UPDATED) {
                        if (data.getLocaleStatus(locale) == UNRESOLVED) {
                            pending.add(new Translation(module, registryFile, key, locale));
                        }
                    }
                }
            }
        }
        for (Translation translation : pending) {
            final String referenceValue = translation.getReferenceValue();
            System.out.println(translation.getKey() + ": " + referenceValue + ": " + translation.getTranslation());
        }

        return null;
    }
    
    public static void main(String[] args) throws Exception {
        final String baseDir = args[0];
        final String locale = args[1];
        new Exporter(new File(baseDir)).export(locale);
    }
    
}
