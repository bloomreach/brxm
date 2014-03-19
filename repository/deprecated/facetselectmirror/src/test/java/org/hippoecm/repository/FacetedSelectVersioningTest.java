/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;

public class FacetedSelectVersioningTest extends RepositoryTestCase {

    String[] content1 = new String[] {
        "/test", "nt:unstructured",
            "/test/doc", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/doc/doc", "hippo:realdocument",
                    "jcr:mixinTypes", "mix:versionable"
    };
    String[] content2 = new String[] {
        "/test/doc/doc/hippo:mirror", "hippo:facetselect",
            //"jcr:mixinTypes", "mix:versionable",
            "hippo:docbase", "/test/doc",
            "hippo:values", null,
            "hippo:facets", null,
            "hippo:modes", null
    };

    @Test
    public void testIssue() throws Exception {
        build(content1, session);
        session.save();
        build(content2, session);
        session.save();
        traverse(session, "/test/doc/doc").checkin();
        session.save();
        //Utilities.dump(System.err, session.getRootNode().getNode("test"));
        assertNotNull(traverse(session, "/test/doc/doc/hippo:mirror/doc"));
    }
}
