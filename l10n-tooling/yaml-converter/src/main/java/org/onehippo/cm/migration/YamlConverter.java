/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.ParseException;

public class YamlConverter {

    public void convertProject(String locale, String outputDir) throws ParseException, EsvParseException, IOException {

        Path hippoEcmExtensionFile = Paths.get(outputDir, locale, "hippoecm-extension.xml");
        if (Files.exists(hippoEcmExtensionFile)) {
            String[] cargs = new String[]{"-s", hippoEcmExtensionFile.getParent().toString(), "-t", hippoEcmExtensionFile.getParent().toString(), "-m", "move"};
            Esv2Yaml.main(cargs);
        } else {
            throw new RuntimeException(hippoEcmExtensionFile + " not found");
        }
    }
}
