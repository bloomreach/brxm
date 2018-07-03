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
package org.onehippo.cm.model.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.geronimo.mail.util.StringBufferOutputStream;
import org.junit.Test;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.serializer.ModuleDescriptorSerializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ModuleDescriptorTest {

    @Test
    public void testLoadSaveFull() throws ParserException, IOException {
        final String resource = "/hcm-module-full.yaml";
        final ModuleImpl module = loadDescriptor(resource);
        writeDescriptor(resource, module);
    }

    @Test
    public void testLoadShort() throws ParserException, IOException {
        final String resource = "/hcm-module-short.yaml";
        ModuleImpl module = loadDescriptor(resource);
        writeDescriptor(resource, module);
    }

    private ModuleImpl loadDescriptor(final String resource) throws ParserException {
        final InputStream stream = getClass().getResourceAsStream(resource);
        final ModuleDescriptorParser moduleDescriptorParser = new ModuleDescriptorParser(true);
        final ModuleImpl module = moduleDescriptorParser.parse(stream, "");
        IOUtils.closeQuietly(stream);
        return module;
    }

    private void writeDescriptor(final String resource, final ModuleImpl module) throws IOException {
        final ModuleDescriptorSerializer serializer = new ModuleDescriptorSerializer(true);
        final StringBuffer out = new StringBuffer();
        final StringBufferOutputStream outputStream = new StringBufferOutputStream(out);
        serializer.serialize(outputStream, module);
        final String original = IOUtils.toString(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
        assertEquals(original, out.toString().trim());
    }

}