/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.model.parser.ParserException;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class SerializerTest extends AbstractBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void read_write_hierarchy_test() throws IOException, ParserException {
        readAndWrite("/parser/hierarchy_test/"+ Constants.HCM_MODULE_YAML);
    }

    @Test
    public void read_write_value_test() throws IOException, ParserException {
        readAndWrite("/parser/value_test/"+Constants.HCM_MODULE_YAML);
    }

    @Test
    public void read_write_explicitly_sequenced() throws IOException, ParserException {
        readAndWrite("/parser/explicitly_sequenced_test/"+Constants.HCM_MODULE_YAML, true);
    }

    @Test
    public void read_write_not_explicitly_sequenced_test() throws IOException, ParserException {
        readAndWrite("/parser/not_explicitly_sequenced_test/"+Constants.HCM_MODULE_YAML, false);
    }

    private void readAndWrite(final String moduleConfig) throws IOException, ParserException {
        readAndWrite(moduleConfig, DEFAULT_EXPLICIT_SEQUENCING);
    }

    private void readAndWrite(final String moduleConfig, final boolean explicitSequencing) throws IOException, ParserException {
        final PathConfigurationReader.ReadResult result = readFromResource(moduleConfig, explicitSequencing);

        final FileConfigurationWriter writer = new FileConfigurationWriter();
        writer.write(folder.getRoot().toPath(), result.getGroups(), result.getModuleContexts(), explicitSequencing);

        final Path expectedRoot = findBase(moduleConfig);
        final Path actualRoot = folder.getRoot().toPath();
        assertNoFileDiff(expectedRoot, actualRoot);
    }

}
