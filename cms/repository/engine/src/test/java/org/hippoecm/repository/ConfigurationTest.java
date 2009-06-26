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
package org.hippoecm.repository;

import static org.junit.Assert.assertTrue;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        if (session.getRootNode().hasNode("hippo:configuration/hippo:initialize/testnode")) {
            session.getRootNode().getNode("hippo:configuration/hippo:initialize/testnode").remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void testConfiguration() throws Exception {
        Node root = session.getRootNode();
        Node node = root.addNode("hippo:configuration/hippo:initialize/testnode", "hippo:initializeitem");
        node.setProperty("hippo:content", "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        node.setProperty("hippo:contentroot", "/test");
        session.save();

        Thread.sleep(3000);
        session.refresh(false);

        assertTrue(root.getNode("test").hasNode("testnode"));
    }
}
