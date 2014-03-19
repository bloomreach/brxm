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

import java.util.Map;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RepositoryMapTest extends RepositoryTestCase {

    private Node root;

    String[] content = new String[] {
        "/content", "nt:unstructured",
            "/content/articles", "hippo:testdocument",
                "jcr:mixinTypes", "mix:versionable",
                "/content/articles/myarticle1", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/content/articles/myarticle1/myarticle1", "hippo:testdocument",
                        "jcr:mixinTypes", "mix:versionable"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Node test;
        if(session.nodeExists("/test"))
            test = session.getNode("/test");
        else
            test = session.getRootNode().addNode("test");
        test.setProperty("aap", "noot");
        test.setProperty("mies", new String[] { "a", "b" });

        if (session.nodeExists("/content")) {
            session.getNode("/content").remove();
            session.save();
        }
        build(content, session);
        root = session.getNode("/content");
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (session.nodeExists("/content")) {
            session.getNode("/content").remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void testMap() throws Exception {
        Map map = (Map) server.getRepositoryMap(root.getNode("articles/myarticle1"));
        map = (Map) map.get("myarticle1");
        assertNotNull(map);
    }

    @Test
    public void testMapProperty() throws Exception {
        Map map = server.getRepositoryMap(session.getNode("/test"));
        assertNotNull(map);
        assertEquals("noot", map.get("aap"));
        String[] mies = (String[]) map.get("mies");
        assertEquals(new String[] { "a", "b" }, mies);
    }

}
