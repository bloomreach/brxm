/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class DocumentViewExportTest extends RepositoryTestCase {
    
    String[] content = new String[] {
        "/test",              "nt:unstructured",
        "jcr:mixinTypes",     "mix:referenceable",
        "/test/fs",           "hippo:facetselect",
        "hippo:docbase",      "/test",
        "hippo:values",       null,
        "hippo:facets",       null,
        "hippo:modes",        null
    };

    @After
    @Override
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        super.tearDown();
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Test
    public void testDocumentViewExportDoesNotRecurseInVirtualLayers() throws Exception {
        build(content, session);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportDocumentView("/test", out, false, false);
    }

}
