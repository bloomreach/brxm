/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class ModuleTest {

    @Test
    public void testIsPathForModule() throws Exception {
        Configuration configuration = new Configuration(true, new HashMap<String, Collection<String>>(), null, null);
        Module foo = new Module("foo", Arrays.asList("/"), new File("foo"), null, null, configuration);
        Module bar = new Module("bar", Arrays.asList("/bar"), new File("bar"), null, null, configuration);
        assertTrue(foo.isPathForModule("/foo"));
        assertTrue(foo.isPathForModule("/foo/bar"));
        assertTrue(foo.isPathForModule("/bar"));
        assertTrue(bar.isPathForModule("/bar"));
        assertTrue(bar.isPathForModule("/bar/quz"));
    }

}
