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

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.Utilities;
import org.junit.Test;

public class MirrorTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static String[] contents = new String[] {
        "/documents", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "niet", "hier",
        "/navigation", "nt:unstructured",
        "/navigation/mirror", "hippo:mirror",
        "hippo:docbase", "/documents",

        "/documents/test1", "nt:unstructured",
        "/documents/test2", "nt:unstructured",
        "wel","anders",
        "/documents/test3", "nt:unstructured",
        "/documents/test3/test4", "nt:unstructured",
        "lachen", "zucht",
        "/documents/test3/test4/test5", "nt:unstructured",
        "/documents/test3/test4/test5", "nt:unstructured"
    };

    @Test public void testMirror() throws Exception {
        PrintStream pstream = new PrintStream("dump.txt");

        build(session, contents);
        session.save();
        session.logout();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        assertNotNull(session.getRootNode());
        assertTrue(session.getRootNode().hasNode("navigation"));
        assertNotNull(session.getRootNode().getNode("navigation"));
        assertTrue(session.getRootNode().getNode("navigation").hasNode("mirror"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasProperty("hippo:docbase"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getProperty("hippo:docbase"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1"));

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        session.getRootNode().addNode("dummy");
        session.getRootNode().getNode("documents").addNode("test-a","nt:unstructured").setProperty("test-b","test-c");
        session.getRootNode().getNode("documents").getNode("test1").addNode("test-x");
        session.save();
        session.refresh(true);

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test-a"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test-a"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").hasNode("test-x"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").getNode("test-x"));
        assertFalse(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1[2]"));
    }
}
