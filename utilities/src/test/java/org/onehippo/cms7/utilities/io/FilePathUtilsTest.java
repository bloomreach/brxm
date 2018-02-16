/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.utilities.io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.utilities.io.FilePathUtils.*;

public class FilePathUtilsTest {

    @Test(expected = IllegalArgumentException.class)
    public void null_path() {
        cleanFilePath(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_name() {
        cleanFileName(null);
    }

    @Test
    public void blank_name() {
        assertEquals(FilePathUtils.DEFAULT_FILE_NAME, cleanFileName("   \t  "));
    }

    @Test
    public void naughty_chars() {
        assertEquals("hippo-resource", cleanFileName("hippo:resource"));
        assertEquals("hippo-resource-sss", cleanFileName(".hippo:<resource>*sss. . . "));
    }

    @Test
    public void port_names() {
        assertEquals(FilePathUtils.DEFAULT_FILE_NAME, cleanFileName("COM1"));
        assertEquals(FilePathUtils.DEFAULT_FILE_NAME, cleanFileName("<>CON.."));
        assertEquals("C-OM1", cleanFileName("C<>OM1"));
    }

    @Test
    public void preserve_extensions() {
        assertEquals("clean.name", cleanFileName("clean.name"));
        assertEquals("weird-name-with.extension", cleanFileName("<>...weird name-with.extension"));

        // we don't preserve extension-only names, if you accidentally send us one!
        assertEquals("onlyextension", cleanFileName(".onlyextension"));
    }
}
