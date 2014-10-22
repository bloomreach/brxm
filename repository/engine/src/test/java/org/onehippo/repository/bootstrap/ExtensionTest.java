/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap;

import java.net.URL;

import javax.jcr.Value;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEFOLDER;
import static org.hippoecm.repository.api.HippoNodeType.TEMPORARY_PATH;
import static org.junit.Assert.assertEquals;

public class ExtensionTest {

    @Test
    public void testGetModuleVersion() {
        final URL url = getClass().getResource("/bootstrap/hippoecm-extension.xml");
        final Extension extension = new Extension(null, url);
        assertEquals("1", extension.getModuleVersion());
    }

    @Test
    public void testUpdateVersionTags() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode initializeFolder = root.addNode(INITIALIZE_PATH, NT_INITIALIZEFOLDER);
        final MockNode temporaryFolder = root.addNode(TEMPORARY_PATH, NT_INITIALIZEFOLDER);
        initializeFolder.setProperty(HIPPO_VERSION, new String[] { "foo" });
        temporaryFolder.setProperty(HIPPO_VERSION, new String[] { "bar", "baz" });
        Extension.updateVersionTags(initializeFolder, temporaryFolder);
        final Value[] values = initializeFolder.getProperty(HIPPO_VERSION).getValues();
        assertEquals(3, values.length);
        assertEquals("foo", values[0].getString());
        assertEquals("bar", values[1].getString());
        assertEquals("baz", values[2].getString());
    }

}
