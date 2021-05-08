/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.serializer.ModuleContext;

public class ConfigurationVerifier {

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("usage: <hcm-module.yaml>\n" +
                    "<hcm-module.yaml>: hcm-module.yaml location");
            return;
        }
        final Path moduleDescriptorPath = Paths.get(args[0]);
        final ModuleContext result = new ModuleReader().read(moduleDescriptorPath, true);

        try (ConfigurationModelImpl configurationModelImpl = new ConfigurationModelImpl()) {
            configurationModelImpl.addModule(result.getModule())
                    .build();
        }
    }
}
