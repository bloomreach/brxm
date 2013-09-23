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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeNameCodecTest extends RepositoryTestCase {

    private static final String TEST_PATH = "testnodes";
    private Node testPath;

    private static final Map<String, String> simpleNames = new HashMap<String, String>();
    private static final Map<String, String> prefixedNames = new HashMap<String, String>();

    static {
        simpleNames.put("1", "1");
        simpleNames.put("1.", "1.");
        simpleNames.put(".1", ".1");
        simpleNames.put("..", "._x002e_");
        simpleNames.put("a", "a");
        simpleNames.put(".a", ".a");
        simpleNames.put("a.", "a.");
        simpleNames.put("a1", "a1");
        simpleNames.put("1a", "1a");
        simpleNames.put("1a1", "1a1");
        simpleNames.put("a1a", "a1a");
        simpleNames.put("a a", "a a");
        simpleNames.put("a@a", "a@a");
        simpleNames.put("a#a", "a#a");
        simpleNames.put(" ", "_x0020_");
        simpleNames.put("\t", "_x0009_");
        simpleNames.put(". ", "._x0020_");
        simpleNames.put(" . ", "_x0020_._x0020_");
        simpleNames.put(" ab", "_x0020_ab");
        simpleNames.put("a b", "a b");
        simpleNames.put("ab ", "ab_x0020_");
        simpleNames.put("@abc", "@abc");
        simpleNames.put("abc@", "abc@");
        simpleNames.put("#abc", "#abc");
        simpleNames.put("#abc#", "#abc#");
        simpleNames.put("_abc", "_abc");
        simpleNames.put("/abc", "_x002f_abc");
        simpleNames.put("a/bc", "a_x002f_bc");
        simpleNames.put("abc/", "abc_x002f_");
        simpleNames.put(":", "_x003a_");
        simpleNames.put("a:", "a_x003a_");
        simpleNames.put("abc:", "abc_x003a_");
        simpleNames.put(":a", "_x003a_a");
        simpleNames.put("a:b", "a_x003a_b");
        simpleNames.put("::", "_x003a__x003a_");
        simpleNames.put(":::", "_x003a__x003a__x003a_");
        simpleNames.put(":::::", "_x003a__x003a__x003a__x003a__x003a_");
        simpleNames.put("a/:[]*'\"|b", "a_x002f__x003a__x005b__x005d__x002a__x0027__x0022__x007c_b");

        prefixedNames.put("hippo:1", "hippo:1");
        prefixedNames.put("hippo:1.", "hippo:1.");
        prefixedNames.put("hippo:.1", "hippo:.1");
        //prefixedNames.put("hippo:..", "hippo:.."); // disabled, JCR valid, but JR won't allow it
        prefixedNames.put("hippo:a", "hippo:a");
        prefixedNames.put("hippo:.a", "hippo:.a");
        prefixedNames.put("hippo:a.", "hippo:a.");
        prefixedNames.put("hippo:a1", "hippo:a1");
        prefixedNames.put("hippo:1a", "hippo:1a");
        prefixedNames.put("hippo:1a1", "hippo:1a1");
        prefixedNames.put("hippo:a1a", "hippo:a1a");
        prefixedNames.put("hippo:a a", "hippo:a a");
        prefixedNames.put("hippo:a@a", "hippo:a@a");
        prefixedNames.put("hippo:a#a", "hippo:a#a");
        prefixedNames.put("hippo: ", "hippo:_x0020_");
        prefixedNames.put("hippo:\t", "hippo:_x0009_");
        prefixedNames.put("hippo:. ", "hippo:._x0020_");
        prefixedNames.put("hippo: . ", "hippo:_x0020_._x0020_");
        prefixedNames.put("hippo: ab", "hippo:_x0020_ab");
        prefixedNames.put("hippo:a b", "hippo:a b");
        prefixedNames.put("hippo:ab ", "hippo:ab_x0020_");
        prefixedNames.put("hippo:@abc", "hippo:@abc");
        prefixedNames.put("hippo:abc@", "hippo:abc@");
        prefixedNames.put("hippo:#abc", "hippo:#abc");
        prefixedNames.put("hippo:#abc#", "hippo:#abc#");
        prefixedNames.put("hippo:_abc", "hippo:_abc");
        prefixedNames.put("hippo:/abc", "hippo:_x002f_abc");
        prefixedNames.put("hippo:a/bc", "hippo:a_x002f_bc");
        prefixedNames.put("hippo:abc/", "hippo:abc_x002f_");
        prefixedNames.put("hippo::", "hippo:_x003a_");
        prefixedNames.put("hippo:a:", "hippo:a_x003a_");
        prefixedNames.put("hippo::a", "hippo:_x003a_a");
        prefixedNames.put("hippo:a:b", "hippo:a_x003a_b");
        prefixedNames.put("hippo:::", "hippo:_x003a__x003a_");
        prefixedNames.put("hippo::::", "hippo:_x003a__x003a__x003a_");
        prefixedNames.put("hippo::::::", "hippo:_x003a__x003a__x003a__x003a__x003a_");
        prefixedNames.put("hippo:a/:[]*'\"|b", "hippo:a_x002f__x003a__x005b__x005d__x002a__x0027__x0022__x007c_b");

    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testSimpleNames() throws RepositoryException {
        for (Entry<String, String> entry : simpleNames.entrySet()) {
            String name = entry.getKey();
            String coded = entry.getValue();
            assertEquals("Failed encoding for: " + name, coded, NodeNameCodec.encode(name, true));
            assertEquals("Failed decoding for: " + coded, name, NodeNameCodec.decode(coded));
            assertEquals("Failed double encoding for: " + name, coded, NodeNameCodec.encode(NodeNameCodec.encode(name, true), true));
            assertEquals("Failed double decoding for: " + coded, name, NodeNameCodec.decode(NodeNameCodec.decode(coded)));
        }
    }

    @Test
    public void testPrefixedNames() throws RepositoryException {
        for (Entry<String, String> entry : prefixedNames.entrySet()) {
            String name = entry.getKey();
            String coded = entry.getValue();
            assertEquals("Failed encoding for: " + name, coded, NodeNameCodec.encode(name));
            assertEquals("Failed decoding for: " + coded, name, NodeNameCodec.decode(coded));
            assertEquals("Failed double encoding for: " + name, coded, NodeNameCodec.encode(NodeNameCodec.encode(name)));
            assertEquals("Failed double decoding for: " + coded, name, NodeNameCodec.decode(NodeNameCodec.decode(coded)));
        }
    }

    @Test
    public void testCreateSimpleNames() throws RepositoryException {
        for (String coded : simpleNames.values()) {
            testPath.addNode(coded);
        }
        session.save();
        for (String coded : simpleNames.values()) {
            assertTrue("Encoded node not found " + coded, testPath.hasNode(coded));
        }
    }

    @Test
    public void testCreatePrefixedNames() throws RepositoryException {
        for (String coded : prefixedNames.values()) {
            testPath.addNode(coded);
        }
        session.save();
        for (String coded : prefixedNames.values()) {
            assertTrue("Encoded node not found " + coded, testPath.hasNode(coded));
        }
    }
}
