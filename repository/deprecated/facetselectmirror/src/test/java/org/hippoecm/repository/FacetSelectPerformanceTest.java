/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

@Ignore
public class FacetSelectPerformanceTest extends RepositoryTestCase {

    String[] content = new String[] {
        "/test",              "nt:unstructured",
        "/test/docs",         "nt:unstructured",
        "jcr:mixinTypes",     "mix:referenceable",
        "/test/docs/doc",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:translated",
        "/test/docs/doc/hippo:translation", "hippo:translation",
        "hippo:message",      "",
        "hippo:language",     "",
        "/test/docs/doc/doc", "hippo:document",
        "jcr:mixinTypes",     "hippostd:relaxed",
        "facet",              "foo"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        String docbase = session.getRootNode().getNode("test").getNode("docs").getIdentifier();
        Node test = session.getRootNode().getNode("test");
        for (int i = 0; i < 5000; i++) {
            Node facet = test.addNode("facet" + i, "hippo:facetselect");
            facet.setProperty("hippo:docbase", docbase);
            facet.setProperty("hippo:facets", new String[] {"facet"});
            facet.setProperty("hippo:values", new String[] {"foo"});
            facet.setProperty("hippo:modes", new String[] {"single"});
        }
        session.save();
        session.refresh(false);
    }
    
    @Test
    public void testFacetSelectPerformance() throws Exception {
        long startTime = System.nanoTime();
        Node test = session.getRootNode().getNode("test");
        for (int i = 0; i < 5000; i++) {
            Node facet = test.getNode("facet" + i);
            facet.getNode("doc");
        }
        long endTime = System.nanoTime();
        System.err.println("Total time: " + (endTime-startTime)/10e5 + " ms");
    }

}
