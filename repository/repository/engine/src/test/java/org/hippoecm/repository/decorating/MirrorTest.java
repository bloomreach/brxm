/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hippoecm.repository.TestCase;

public class MirrorTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static String[] contents1 = new String[] {
        "/documents", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "niet", "hier",
        "/documents/test1", "nt:unstructured",
        "/documents/test2", "nt:unstructured",
        "wel","anders",
        "/documents/test3", "nt:unstructured",
        "/documents/test3/test4", "nt:unstructured",
        "lachen", "zucht",
        "/documents/test3/test4/test5", "nt:unstructured",
    };

    private static String[] contents2 = new String[] {
        "/navigation", "nt:unstructured",
        "/navigation/mirror", "hippo:mirror",
        "hippo:docbase", "/documents"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if(session.getRootNode().hasNode("documents")) {
            session.getRootNode().getNode("documents").remove();
        }
        if(session.getRootNode().hasNode("navigation")) {
            session.getRootNode().getNode("navigation").remove();
        }
        build(session, contents1);
        session.save();
        build(session, contents2);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("documents")) {
            session.getRootNode().getNode("documents").remove();
        }
        if(session.getRootNode().hasNode("navigation")) {
            session.getRootNode().getNode("navigation").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testMirror() throws Exception {
        assertNotNull(session.getRootNode());
        assertTrue(session.getRootNode().hasNode("navigation"));
        assertNotNull(session.getRootNode().getNode("navigation"));
        assertTrue(session.getRootNode().getNode("navigation").hasNode("mirror"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasProperty("hippo:docbase"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getProperty("hippo:docbase"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1"));

        session.getRootNode().addNode("dummy");
        session.getRootNode().getNode("documents").addNode("test-a","nt:unstructured").setProperty("test-b","test-c");
        session.getRootNode().getNode("documents").getNode("test1").addNode("test-x");
        session.save();
        session.refresh(true);

        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test-a"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test-a"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").hasNode("test-x"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").getNode("test-x"));
        assertFalse(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1[2]"));
    }
}
