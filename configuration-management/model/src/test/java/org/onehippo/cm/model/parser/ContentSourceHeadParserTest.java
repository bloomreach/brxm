/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContentSourceHeadParserTest extends AbstractBaseTest {

    @Test
    public void testComposeYamlHeadExplicitSequencing() throws Exception {
        final String yaml =
                  "/content/documents/myhippoproject:\n"
                + "  - .meta:order-before: test\n"
                + "  - no-meta: dummy\n"
                + "  - /foo: \n"
                + "      bar: asd\n";

        final ContentSourceHeadParser parser = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, true);
        final Pair<Node, List<NodeTuple>> result = parser.composeYamlHead(IOUtils.toInputStream(yaml, "UTF-8"), "location");
        assertEquals("/content/documents/myhippoproject", ((ScalarNode)result.getKey()).getValue());
        assertEquals(1, result.getValue().size());
        assertEquals(".meta:order-before", ((ScalarNode)result.getValue().get(0).getKeyNode()).getValue());
        assertEquals("test", ((ScalarNode)result.getValue().get(0).getValueNode()).getValue());
    }

    @Test
    public void testComposeYamlHeadNotExplicitSequencing() throws Exception {
        final String yaml =
                "/content/documents/myhippoproject:\n"
                        + "  .meta:order-before: test\n"
                        + "  no-meta: dummy\n"
                        + "  /foo: \n"
                        + "    bar: asd\n";

        final ContentSourceHeadParser parser = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        final Pair<Node, List<NodeTuple>> result = parser.composeYamlHead(IOUtils.toInputStream(yaml, "UTF-8"), "location");
        assertEquals("/content/documents/myhippoproject", ((ScalarNode)result.getKey()).getValue());
        assertEquals(1, result.getValue().size());
        assertEquals(".meta:order-before", ((ScalarNode)result.getValue().get(0).getKeyNode()).getValue());
        assertEquals("test", ((ScalarNode)result.getValue().get(0).getValueNode()).getValue());
    }

    @Test
    public void testComposeYamlHeadWithUnsupportedNodeAlias() throws Exception {
        final String yaml =
                "/content/documents/myhippoproject:\n"
                        + "  .meta:order-before: *alias\n"
                        + "  no-meta: dummy\n"
                        + "  /foo: \n"
                        + "    bar: asd\n";

        final ContentSourceHeadParser parser = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        try {
        parser.composeYamlHead(IOUtils.toInputStream(yaml, "UTF-8"), "location");
            fail("parsing should have failed on encountered node alias");
        } catch (ParserException e) {
            assertTrue(e.getMessage().startsWith("Encounter node alias 'alias' which is not supported when parsing a document head only"));
        }
    }

    @Test
    public void testComposeYamlHeadWithUnsupportedAnchor() throws Exception {
        final String yaml =
                "/content/documents/myhippoproject: &anchor\n"
                        + "  .meta:order-before: test\n"
                        + "  no-meta: dummy\n"
                        + "  /foo: \n"
                        + "    bar: asd\n";

        final ContentSourceHeadParser parser = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        try {
            parser.composeYamlHead(IOUtils.toInputStream(yaml, "UTF-8"), "location");
            fail("parsing should have failed on encountered anchor");
        } catch (ParserException e) {
            assertTrue(e.getMessage().startsWith("Encountered node anchor 'anchor' which is not supported when parsing a document head only"));
        }
    }

    @Test
    public void testParseHead() throws Exception {
        final String yaml =
                "/content/documents/myhippoproject:\n"
                        + "  .meta:order-before: test\n"
                        + "  no-meta: dummy\n"
                        + "  /foo: \n"
                        + "    bar: foobar\n";
        final GroupImpl c1 = new GroupImpl("c1");
        final ProjectImpl p1 = c1.addProject("p1");
        final ModuleImpl m1 = p1.addModule("m1");
        final ModuleImpl m2 = p1.addModule("m2");

        final ContentSourceHeadParser parser1 = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        parser1.parse(IOUtils.toInputStream(yaml, "UTF-8"), "location", "location", m1);
        m1.build();

        final ContentSourceParser parser2 = new ContentSourceParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        parser2.parse(IOUtils.toInputStream(yaml, "UTF-8"), "location", "location", m2);
        m2.build();

        assertEquals(1, m1.getContentDefinitions().size());
        final ContentDefinitionImpl d1 = m1.getContentDefinitions().get(0);
        assertEquals("/content/documents/myhippoproject", d1.getRootPath().toString());
        assertEquals("test", d1.getNode().getOrderBefore());
        assertTrue(d1.getNode().getProperties().isEmpty());
        assertTrue(d1.getNode().getNodes().isEmpty());

        assertEquals(1, m2.getContentDefinitions().size());
        final ContentDefinitionImpl d2 = m2.getContentDefinitions().get(0);
        assertEquals("/content/documents/myhippoproject", d2.getRootPath().toString());
        assertEquals("test", d2.getNode().getOrderBefore());
        assertFalse(d2.getNode().getProperties().isEmpty());
        assertFalse(d2.getNode().getNodes().isEmpty());
    }

    /*
    The following test is disabled (ignored) as it expects a huge (library.yaml) test resource file which can be
    copied manually under src/test/resources if desired, but should *not* be checked in!
     */
    @Test @Ignore
    public void testHugeContentHead() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ProjectImpl p1 = c1.addProject("p1");
        final ModuleImpl m1 = p1.addModule("m1");
        final ModuleImpl m2 = p1.addModule("m2");

        final StopWatch stopWatch = new StopWatch();
        final ContentSourceHeadParser parser1 = new ContentSourceHeadParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        stopWatch.start();
        parser1.parse(this.getClass().getResourceAsStream("/library.yaml"), "location", "location", m1);
        m1.build();
        final ContentDefinitionImpl d1 = m1.getContentDefinitions().get(0);
        assertEquals("trails", d1.getNode().getOrderBefore());
        stopWatch.stop();
        System.out.println("loading huge content head only: "+stopWatch.toString());

        final ContentSourceParser parser2 = new ContentSourceParser(DUMMY_RESOURCE_INPUT_PROVIDER, false);
        stopWatch.reset();
        stopWatch.start();
        parser2.parse(this.getClass().getResourceAsStream("/library.yaml"), "location", "location", m2);
        m2.build();
        final ContentDefinitionImpl d2 = m2.getContentDefinitions().get(0);
        assertEquals("trails", d2.getNode().getOrderBefore());
        stopWatch.stop();
        System.out.println("loading huge content: "+stopWatch.toString());
    }
}
