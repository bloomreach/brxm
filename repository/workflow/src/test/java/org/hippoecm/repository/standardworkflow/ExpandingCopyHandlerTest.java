/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpandingCopyHandlerTest extends RepositoryTestCase {

    private Node source;
    private Node target;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session.getRootNode().addNode("test");
        source = test.addNode("source");
        source.setProperty("prop", "value");
        source.setProperty("multiprop", new String[] { "value1", "value2" });
        source.addNode("child").setProperty("prop", "value");
        target = test.addNode("target");
    }

    @Test
    public void testCopyWithNodeNameSubstitutes() throws Exception {
        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./_name", new String[] { "substitute" });
            put("./child/_name", new String[] { "substitute" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(source, handler);
        assertTrue(session.nodeExists("/test/target/substitute"));
        assertTrue(session.nodeExists("/test/target/substitute/substitute"));
    }

    @Test
    public void testCopyWithPropertyValueSubstitutes() throws Exception {
        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./prop", new String[] { "substitute" });
            put("./child/prop", new String[] { "substitute" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(source, handler);
        assertEquals("substitute", session.getProperty("/test/target/source/prop").getString());
        assertEquals("substitute", session.getProperty("/test/target/source/child/prop").getString());
    }

    @Test
    public void testCopyNodeNameAndPropertyValueSubstitutesAlsoMatchesWildcard() throws Exception {
        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./_node/_name", new String[] { "substitute" });
            put("./_node/prop", new String[] { "substitute" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(source, handler);
        assertTrue(session.nodeExists("/test/target/source/substitute"));
        assertEquals("substitute", session.getProperty("/test/target/source/substitute/prop").getString());
    }

    @Test
    public void testCopyWithMultiPropertyValueSubstitution() throws Exception {
        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./multiprop", new String[] { "substitute1", "substitute2" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(source, handler);
        final Value[] values = session.getProperty("/test/target/source/multiprop").getValues();
        assertEquals(2, values.length);
        assertEquals("substitute1", values[0].getString());
        assertEquals("substitute2", values[1].getString());
    }

    @Test
    public void testCopyWithMultiPropertyValueIndexedSubstitution() throws Exception {
        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./multiprop[1]", new String[] { "substitute" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(source, handler);
        final Value[] values = session.getProperty("/test/target/source/multiprop").getValues();
        assertEquals(2, values.length);
        assertEquals("substitute", values[1].getString());
    }

    @Test
    public void testNodeIsNotRenamedIfDefinitionDisallowsIt() throws Exception {
        Node handle = session.getNode("/test").addNode("handle", "hippo:handle");
        handle.addMixin("hippo:translated");
        final Node translation = handle.addNode("hippo:translation", "hippo:translation");
        translation.setProperty("hippo:language", "test");
        translation.setProperty("hippo:message", "test");
        handle.addNode("document", "hippo:document");

        final Map<String, String[]> substitutes = new HashMap<String, String[]>() {{
            put("./_node/_name", new String[] { "substitute" });
        }};
        ExpandingCopyHandler handler = new ExpandingCopyHandler(target, substitutes, session.getValueFactory());
        JcrUtils.copyTo(handle, handler);
        // according to the substitution pattern hippo:translation node should have been renamed
        // but this doesn't happen because it would violate the constraints on the handle node
        assertTrue(session.nodeExists("/test/target/handle/hippo:translation"));
        // the document node at the same level does get renamed
        assertTrue(session.nodeExists("/test/target/handle/substitute"));
        assertEquals("hippo:translation", session.getNode("/test/target/handle/hippo:translation").getPrimaryNodeType().getName());
        assertEquals("hippo:document", session.getNode("/test/target/handle/substitute").getPrimaryNodeType().getName());
    }
}
