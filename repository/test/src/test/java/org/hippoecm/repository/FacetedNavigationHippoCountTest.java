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

import static org.junit.Assert.assertTrue;

public class FacetedNavigationHippoCountTest extends RepositoryTestCase
{

    private static String[] contents1 = new String[] {
        "/test", "nt:unstructured",
        "/test/documents", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/document", "hippo:testdocument",
        "type", "text",
        "/test/documents/document1", "hippo:testdocument",
        "type", "html"
    };
    private static String[] contents2 = new String[] {
        "/test/docsearch", "nt:unstructured",
        "/test/docsearch/byType", "hippo:facetsearch",
        "hippo:docbase", "/test/documents",
        "hippo:queryname", "byType",
        "hippo:facets", "type"
    };

    @Test
    public void testHippoCount() throws Exception {
        build(contents1, session);
        session.save();
        build(contents2, session);
        session.save();
        assertTrue(traverse(session, "/test/docsearch/byType/hippo:resultset").hasProperty("hippo:count"));
    }
}
