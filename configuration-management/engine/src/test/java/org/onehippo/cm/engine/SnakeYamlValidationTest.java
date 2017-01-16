/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.engine;

import java.io.StringReader;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.ComposerException;
import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * This test pins down some validation behaviour of SnakeYAML, e.g. what kind of exceptions are thrown in case if
 * invalid or unexpected input. We pin down this behaviour in order to get a trigger if a newer version of SnakeYAML
 * decides to change this behaviour.
 */
public class SnakeYamlValidationTest extends AbstractBaseTest {

    final Yaml yamlParser = new Yaml();

    @Test
    public void invalidSyntax() {
        try {
            yamlParser.compose(new StringReader("This is not valid YAML:"));
        } catch (ScannerException ignored) { }
    }

    @Test
    public void unexpectedMultiDocumentFile() {
        final String multiDocumentYaml = "---\n"
            + "- Tobi\n"
            + "- Oscar\n"
            + "---\n"
            + "Another document\n"
            + "---\n"
            + "color: blue\n"
            + "size: large";

        try {
            yamlParser.compose(new StringReader(multiDocumentYaml));
        } catch (ComposerException ignored) { }
    }

    @Test
    public void multiDocumentFileWithInvalidSyntax() {
        final String invalidMultiDocumentYaml = "---\n"
                + "- Tobi\n"
                + "- Oscar\n"
                + "---\n"
                + "Another document:\n"
                + "---\n"
                + "color: blue\n"
                + "size: large";

        try {
            yamlParser.composeAll(new StringReader(invalidMultiDocumentYaml));
        } catch (ScannerException ignored) { }
    }
}
