/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.util.jar.Manifest;

import org.hippoecm.repository.HippoRepository;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.hippoecm.repository.util.RepoUtils.encodeXpath;

public class RepoUtilsTest {

    @Test
    public void testGetManifest() throws Exception {
        final Manifest manifest = RepoUtils.getManifest(HippoRepository.class);
        assertNotNull(manifest);
        assertEquals("Repository API", manifest.getMainAttributes().getValue("Implementation-Title"));
    }

    @Test
    public void testISO9075EncodeXPaths() {
        assertEquals("/jcr:root/foo/bar", encodeXpath("/jcr:root/foo/bar"));
        assertEquals("/jcr:root/foo/_x0037_8", encodeXpath("/jcr:root/foo/78"));
        assertEquals("/jcr:root/foo/bar[@my:project = '456']", encodeXpath("/jcr:root/foo/bar[@my:project = '456']"));
        assertEquals("/jcr:root/foo/_x0037_8[@my:project = '456']", encodeXpath("/jcr:root/foo/78[@my:project = '456']"));
        assertEquals("/jcr:root/foo/_x0037_8[test/@my:project = '456']", encodeXpath("/jcr:root/foo/78[test/@my:project = '456']"));

        // we do not encode *IN* where clauses
        assertEquals("/jcr:root/foo/_x0037_8[99/@my:project = '456']", encodeXpath("/jcr:root/foo/78[99/@my:project = '456']"));

        assertEquals("//element(*,hippo:document)", encodeXpath("//element(*,hippo:document)"));
        assertEquals("/jcr:root/foo/bar//element(*,hippo:document)", encodeXpath("/jcr:root/foo/bar//element(*,hippo:document)"));
        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)",  encodeXpath("/jcr:root/foo/78//element(*,hippo:document)"));

        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)[@my:project = 'test']",
                encodeXpath("/jcr:root/foo/78//element(*,hippo:document)[@my:project = 'test']"));

        // we do not encode *IN* where clauses
        assertEquals("/jcr:root/foo/_x0037_8//element(*,hippo:document)[99/@my:project = 'test']",
                encodeXpath("/jcr:root/foo/78//element(*,hippo:document)[99/@my:project = 'test']"));

        assertEquals("//*", encodeXpath("//*"));
        assertEquals("//*[jcr:contains(.,'test')]", encodeXpath("//*[jcr:contains(.,'test')]"));
        assertEquals("//element(*,hippo:document)", encodeXpath("//element(*,hippo:document)"));

        assertEquals(       "/jcr:root/foo/bar order by @jcr:score",
                encodeXpath("/jcr:root/foo/bar order by @jcr:score"));
        assertEquals(       "/jcr:root/foo/bar[@my:project = '456'] order by @jcr:score",
                encodeXpath("/jcr:root/foo/bar[@my:project = '456'] order by @jcr:score"));

        assertEquals(       "/jcr:root/foo/_x0037_8 order by @jcr:score",
                encodeXpath("/jcr:root/foo/78 order by @jcr:score"));
        assertEquals(       "/jcr:root/foo/_x0037_8[99/@my:project = '456'] order by @jcr:score",
                encodeXpath("/jcr:root/foo/78[99/@my:project = '456'] order by @jcr:score"));


    }

}
