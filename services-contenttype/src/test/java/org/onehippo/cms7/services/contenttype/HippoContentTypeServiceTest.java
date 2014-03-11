/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.codehaus.jackson.map.ObjectMapper;
import org.hippoecm.frontend.PluginTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class HippoContentTypeServiceTest extends PluginTest {

    private ContentTypeServiceModule serviceModule;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode("hippo:namespaces/test")) {
            session.getRootNode().getNode("hippo:namespaces/test").remove();
            session.save();
            session.refresh(false);
        }
        session.importXML("/hippo:namespaces", getClass().getClassLoader().getResourceAsStream("test-namespace.xml"),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        session.save();
        session.refresh(false);
        serviceModule = new ContentTypeServiceModule();
        serviceModule.initialize(session);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        serviceModule.shutdown();
        if (session != null ) {
            if (session.getRootNode().hasNode("hippo:namespaces/test")) {
                session.getRootNode().getNode("hippo:namespaces/test").remove();
                session.save();
                session.refresh(false);
            }
            if (session.getRootNode().hasNode("testNode")) {
                session.getRootNode().getNode("testNode").remove();
                session.save();
                session.refresh(false);
            }
        }
        super.tearDown();
    }

    @Test
    public void testEffectiveNodeTypesSealed() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            EffectiveNodeTypes entCache = service.getEffectiveNodeTypes();
            try {
                entCache.getTypesByPrefix().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getTypesByPrefix().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getTypesByPrefix().get("nt").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getTypesByPrefix().get(nt).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getSuperTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getSuperTypes().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getChildren().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getChildren().get("jcr:content").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().get(jcr:content).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getChildren().get("jcr:content").get(0).getRequiredPrimaryTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().get(jcr:content).get(0).getRequiredPrimaryTypes().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getProperties().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").get(0).getDefaultValues().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).get(0).getDefaultValues().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").get(0).getValueConstraints().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).get(0).getValueConstraints().clear()");
            } catch (UnsupportedOperationException uoe) {}
        }
    }

    @Test
    public void testEffectiveNodeTypesModel() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            EffectiveNodeTypes entCache = service.getEffectiveNodeTypes();
            assertTrue(entCache.getTypesByPrefix().keySet().contains("nt"));

            EffectiveNodeType t = entCache.getType("nt:file");
            assertNotNull(t);
            assertEquals("EffectiveNodeType(nt:file).getName()", "nt:file", t.getName());
            assertEquals("EffectiveNodeType(nt:file).getChildren().size()", 1, t.getChildren().size());
            assertEquals("EffectiveNodeType(nt:file).getChildren().get(jcr:content).size()", 1, t.getChildren().get("jcr:content").size());
            assertEquals("EffectiveNodeType(nt:file).getChildren().get(jcr:content).get(0).getRequiredPrimaryTypes().iterator().next()",
                    "nt:base", t.getChildren().get("jcr:content").get(0).getRequiredPrimaryTypes().iterator().next());
            assertEquals("EffectiveNodeType(nt:file).getProperties().get(jcr:createdBy).get(0).getDefiningType()",
                    "mix:created", t.getProperties().get("jcr:createdBy").get(0).getDefiningType());
        }
    }

    @Test
    public void testEffectiveNodeType2Json() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            EffectiveNodeTypes entCache = service.getEffectiveNodeTypes();
            EffectiveNodeType t = entCache.getType("nt:file");

            assertNotNull(t);

            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(t);
            // Note: when updating the nt-file.json file, make sure not to (re)format it and that it doesn't have any trailing (empty) lines.
            //       The purpose of the literal comparison here, instead of a logical one by just comparing their json
            //       structure is to ensure the output is actually also in a readable order, and remains so.
            String jsonFromFile = new java.util.Scanner(getClass().getResourceAsStream("nt-file.json")).useDelimiter("\\A").next();
            if (!json.equals(jsonFromFile)) {
                boolean showJson = true;
                if (showJson) {
                    fail("JSON serialization for nt:file doesn't match content for nt-file.json. JSON output: \n"+json+"\n");
                }
                fail("JSON serialization for nt:file doesn't match content for nt-file.json");
            }
        }
    }

    @Test
    public void testContentTypesSealed() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            ContentTypes ctCache = service.getContentTypes();
            try {
                ctCache.getTypesByPrefix().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getTypesByPrefix().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getTypesByPrefix().get("test").clear();
                fail("UnsupportedOperationException expected for ContentTypes.getTypesByPrefix().get(test).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getSuperTypes().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getType(test:test).getSuperTypes().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getChildren().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getType(test:test).getChildren().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getProperties().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getType(test:test).getProperties().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getProperties().get("test:title").getItemProperties().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getType(test:test).getProperties().get(test:title).getItemProperties().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getItem("test:title").getValidators().clear();
                fail("UnsupportedOperationException expected for ContentTypes.getType(test:test).getItem(test:title).getValidators().clear()");
            } catch (UnsupportedOperationException uoe) {}

            // repeat sealed check for EffectiveNodeType underlying the ContentType
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getSuperTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getSuperTypes().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getChildren().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getChildren().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getChildren().get("test:child").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getChildren().get(test:child).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getChildren().get("test:child").get(0).getRequiredPrimaryTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getChildren().get(test:child).get(0).getRequiredPrimaryTypes().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getProperties().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getProperties().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getProperties().get("test:title").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getProperties().get(test:title).clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getProperties().get("test:title").get(0).getDefaultValues().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getProperties().get(test:title).get(0).getDefaultValues().clear()");
            } catch (UnsupportedOperationException uoe) {}
            try {
                ctCache.getType("test:test").getEffectiveNodeType().getProperties().get("test:title").get(0).getValueConstraints().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(test:test).getProperties().get(test:title).get(0).getValueConstraints().clear()");
            } catch (UnsupportedOperationException uoe) {}
        }
    }

    @Test
    public void testContentType() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            ContentTypes ctCache = service.getContentTypes();
            ContentType ct = ctCache.getType("test:test");

            assertNotNull(ct);
            assertEquals(3, ct.getProperties().size());
            assertEquals(1, ct.getChildren().size());
            assertEquals(1, ct.getAggregatedTypes().size());
            assertTrue(!ct.getSuperTypes().contains("hippostd:container"));

            Node testNodeType = session.getNode("/hippo:namespaces/test/test/hipposysedit:nodetype/hipposysedit:nodetype[2]");
            Node extraField = testNodeType.addNode("extraField", "hipposysedit:field");
            extraField.setProperty("hipposysedit:path", "test:extraField");
            extraField.setProperty("hipposysedit:type", "String");
            session.save();
            session.refresh(false);

            // need to wait a bit to get Jackrabbit to refresh and notify the changes
            Thread.sleep(1000);
            ctCache = service.getContentTypes();
            ct = ctCache.getType("test:test");

            // added test:extraField shouldn't be merged as there is no matching property in the EffectiveNodeType
            assertEquals(3, ct.getProperties().size());
            assertTrue(!ct.getProperties().containsKey("test:extraField"));

            // adding relaxed mixin should expose and 'enable' the extraField

            testNodeType.setProperty("hipposysedit:supertype", new String[]{"hippostd:relaxed"});
            session.save();
            session.refresh(false);
            // need to wait a bit to get Jackrabbit to refresh and notify the changes
            Thread.sleep(1000);
            ctCache = service.getContentTypes();
            ct = ctCache.getType("test:test");

            assertEquals(4, ct.getProperties().size());
            assertTrue(ct.getProperties().containsKey("test:extraField"));
            assertTrue(ct.getItem("test:extraField").getEffectiveNodeTypeItem().getName().equals("*"));
            assertTrue(ct.getItem("test:extraField").getEffectiveNodeTypeItem().getDefiningType().equals("hippostd:relaxed"));
            assertTrue(ct.getAggregatedTypes().contains("hippostd:relaxed"));
            assertTrue(ct.getSuperTypes().contains("hippostd:container"));

            session.getRootNode().addNode("testNode", "test:test");
            session.save();

            ct = service.getContentTypes().getContentTypeForNodeByPath(session, "/testNode");

            assertEquals(3, ct.getProperties().size());
            assertEquals(1, ct.getChildren().size());
            assertEquals(1, ct.getAggregatedTypes().size());

            session.getNode("/testNode").addMixin("hippostd:relaxed");
            session.save();

            ct = service.getContentTypes().getContentTypeForNodeByPath(session, "/testNode");
            assertEquals(4, ct.getProperties().size());
            assertTrue(ct.getProperties().containsKey("test:extraField"));
            assertTrue(ct.getAggregatedTypes().contains("hippostd:relaxed"));
            assertTrue(ct.getSuperTypes().contains("hippostd:container"));

            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ct);

            // Note: when updating the testNode.json file, make sure not to (re)format it and that it doesn't have any trailing (empty) lines.
            //       The purpose of the literal comparison, instead of a logical one by just comparing their json
            //       structure is to ensure the output is actually also in a readable order, and remains so.
            String jsonFromFile = new java.util.Scanner(getClass().getResourceAsStream("testNode.json")).useDelimiter("\\A").next();
            if (!json.equals(jsonFromFile)) {
                boolean showJson = true;
                if (showJson) {
                    fail("JSON serialization for testNode doesn't match content for testNode.json. JSON output: \n"+json+"\n");
                }
                fail("JSON serialization for testNode doesn't match content for testNode.json");
            }
        }
    }

    @Test
    public void testRecursiveSuperType() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            ContentTypes ctCache = service.getContentTypes();
            ContentType ct = ctCache.getType("test:test");

            assertNotNull(ct);
            assertTrue(!ct.isDerivedType());
            assertTrue(!ct.getSuperTypes().contains("test:inheriting"));

            ct = ctCache.getType("test:inheriting");

            assertNotNull(ct);
            assertTrue(!ct.isDerivedType());
            assertTrue(ct.getSuperTypes().contains("test:test"));
            assertTrue(ct.isContentType("test:test"));

            // add circular super type inheritance between edited and inheritingfromedited
            Node testNodeType = session.getNode("/hippo:namespaces/test/test/hipposysedit:nodetype/hipposysedit:nodetype[2]");
            testNodeType.setProperty("hipposysedit:supertype", new String[]{"test:inheriting"});
            session.save();
            session.refresh(false);

            // need to wait a bit to get Jackrabbit to refresh and notify the changes
            Thread.sleep(1000);
            ctCache = service.getContentTypes();

            ct = ctCache.getType("test:test");

            assertNotNull(ct);

            // check circular inheritance is 'repaired' by fallback to underlying derived types
            assertTrue(ct.isDerivedType());
            assertTrue(!ct.getSuperTypes().contains("test:inheriting"));

            ct = ctCache.getType("test:inheriting");

            assertNotNull(ct);
            assertTrue(ct.isDerivedType());
            assertTrue(ct.getSuperTypes().contains("test:test"));
            assertTrue(ct.isContentType("test:test"));
        }
    }

    @Test
    public void testNodeTypeChangeListener() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            EffectiveNodeTypes entCache1 = service.getEffectiveNodeTypes();
            ContentTypes ctCache1 = service.getContentTypes();
            session.getRootNode().getNode("hippo:namespaces/test").remove();
            session.save();
            session.refresh(false);
            NodeTypeManager ntm = session.getWorkspace().getNodeTypeManager();
            NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntm.getNodeType("test:inheriting"));
            ntt.setQueryable(!ntt.isQueryable());
            // trigger update
            ntm.registerNodeType(ntt, true);
            EffectiveNodeTypes entCache2 = service.getEffectiveNodeTypes();
            if (entCache1.version() == entCache2.version()) {
                fail("EffectiveNodeTypes cache should have been reloaded.");
            }
            ContentTypes ctCache2 = service.getContentTypes();
            if (ctCache1.version()==ctCache2.version()) {
                fail("ContentTypes cache should have been reloaded.");
            }
            session.getRootNode().addNode("hippo:namespaces/test", "hipposysedit:namespace");
            session.save();
            // need to wait a bit to get Jackrabbit to refresh and notify the changes
            Thread.sleep(1000);
            ctCache1 = service.getContentTypes();
            if (ctCache1.version()==ctCache2.version()) {
                fail("ContentTypes cache should have been reloaded.");
            }
            if (ctCache1.getEffectiveNodeTypes().version() != ctCache2.getEffectiveNodeTypes().version()) {
                // disabled failure: this normally won't fail, but without exact control over or blocking of possible other
                // background processes within the repository, it cannot be guaranteed no other change might have triggered an event
                // on /jcr:system/jcr:nodeTypes, which thus can result in (also) the effectiveNodeTypes been reloaded.

                // fail("EffectiveNodeTypes cache should not have been reloaded.");
            }
        }
    }
}
