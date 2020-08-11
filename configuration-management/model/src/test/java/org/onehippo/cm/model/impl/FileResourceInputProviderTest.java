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
package org.onehippo.cm.model.impl;

import java.nio.file.Paths;

import org.junit.Test;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.util.FilePathUtils;

import static org.junit.Assert.assertEquals;

public class FileResourceInputProviderTest {

    @Test
    public void testFullSourcePath() {
        FileResourceInputProvider rip = new FileResourceInputProvider(Paths.get("base"), "hcm-config");
        ModuleImpl module = new ModuleImpl("m", new ProjectImpl("p", new GroupImpl("g")));
        SourceImpl source = new ConfigSourceImpl("source.yaml", module);
        assertEquals("base/hcm-config/source.yaml", FilePathUtils.unixPath(rip.getFullSourcePath(source)));
    }
}
