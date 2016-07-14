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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.onehippo.cms.l10n.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public class Exporter {

    private final File baseDir;
    private final ExportFileWriter exportFileWriter;

    Exporter(final File baseDir, final ExportFileWriter exportFileWriter) {
        this.baseDir = baseDir;
        this.exportFileWriter = exportFileWriter;
    }

    File export(final String locale) throws IOException {
        return export(locale, false);
    }

    File export(final String locale, final boolean full) throws IOException {
        final List<String[]> exportData = new ArrayList<>();
        final String englishHeader = Locale.ENGLISH.getDisplayName(new Locale(locale));
        final String translationLanguageHeader = new Locale(locale).getDisplayName(new Locale(locale));
        exportData.add(new String[] { "Key", englishHeader, translationLanguageHeader });

        for (Module module : new ModuleLoader(baseDir).loadModules()) {
            final Registry registry = module.getRegistry();
            for (final RegistryInfo registryInfo : registry.getRegistryInfos()) {
                registryInfo.load();
                for (final String key : registryInfo.getKeys()) {
                    final KeyData data = registryInfo.getKeyData(key);

                    if (full) {
                        final Translation translation = new Translation(module, registryInfo, key, locale);
                        exportData.add(new String[] { translation.getFQKey(), translation.getReferenceValue(), translation.getTranslation() });
                    } else {
                        if (data.getStatus() == ADDED || data.getStatus() == UPDATED) {
                            if (data.getLocaleStatus(locale) == UNRESOLVED) {
                                final Translation translation = new Translation(module, registryInfo, key, locale);
                                exportData.add(new String[] { translation.getFQKey(), translation.getReferenceValue(), translation.getTranslation() });
                            }
                        }
                    }
                }
            }
        }

        final File exportFile = new File(baseDir, "export_" + locale + "." + exportFileWriter.getFileExtension());
        exportFileWriter.write(exportFile, exportData);

        return exportFile;
    }
    
}
