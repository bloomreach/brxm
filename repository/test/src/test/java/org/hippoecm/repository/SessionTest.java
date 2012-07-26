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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SessionTest extends TestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMultiValuedPropertyCopied() throws Exception {
        build(session, new String[] {
            "/test", "nt:unstructured",
                "/test/node", "hippo:testrelaxed",
        });
        session.save();
        session.refresh(false);
        Node node = session.getRootNode().getNode("test/node");
        node.setProperty("string", new Value[0], PropertyType.STRING);
        node.setProperty("double", new Value[0], PropertyType.DOUBLE);
        session.save();
        Node copy = ((HippoSession) session).copy(node, "/test/copy");
        assertEquals(PropertyType.STRING, copy.getProperty("string").getType());
        assertEquals(PropertyType.DOUBLE, copy.getProperty("double").getType());
    }
}
