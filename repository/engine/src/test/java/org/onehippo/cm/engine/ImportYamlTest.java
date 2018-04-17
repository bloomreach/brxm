/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Test;
import org.onehippo.cm.model.ImportModuleContext;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.parser.PathConfigurationReader;
import org.onehippo.cm.model.serializer.ModuleContext;

import static org.junit.Assert.assertTrue;

public class ImportYamlTest {

    @Test
    public void importYamlTest() throws Exception {
        final ModuleImpl module = new ModuleImpl("import-module", new ProjectImpl("import-project",
                new GroupImpl("import-group")));
        final File file = new File(this.getClass().getClassLoader().getResource("yaml-import").getFile());
        final ModuleContext moduleContext = new ImportModuleContext(module, Paths.get(file.getAbsolutePath()));
        try {
            new PathConfigurationReader().readModule(module, moduleContext, false);
            final Optional<SourceImpl> source = module.getSources().stream()
                    .filter(s -> s.getPath().equals("yaml-import-test.yaml")).findFirst();
            assertTrue(source.isPresent());
        } catch (Exception e) {
            throw new Exception("Importing yaml sources failed");
        }
    }
}
