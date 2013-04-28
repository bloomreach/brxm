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
        if (session != null && session.getRootNode().hasNode("hippo:namespaces/test")) {
            session.getRootNode().getNode("hippo:namespaces/test").remove();
            session.save();
            session.refresh(false);
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
                entCache.getTypesByPrefix().get("nt").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getTypesByPrefix().get(nt).clear()");
                entCache.getType("nt:file").getSuperTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getSuperTypes().clear()");
                entCache.getType("nt:file").getChildren().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().clear()");
                entCache.getType("nt:file").getChildren().get("jcr:content").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().get(jcr:content).clear()");
                entCache.getType("nt:file").getChildren().get("jcr:content").get(0).getRequiredPrimaryTypes().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getChildren().get(jcr:content).get(0).getRequiredPrimaryTypes().clear()");
                entCache.getType("nt:file").getProperties().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().clear()");
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).clear()");
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").get(0).getDefaultValues().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).get(0).getDefaultValues().clear()");
                entCache.getType("nt:file").getProperties().get("jcr:primaryType").get(0).getValueConstraints().clear();
                fail("UnsupportedOperationException expected for EffectiveNodeTypes.getType(nt:file).getProperties().get(jcr:primaryType).get(0).getValueConstraints().clear()");
            }
            catch (UnsupportedOperationException uoe) {
                // OK
            }
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

//    @Test
    public void testDocumentType2Json() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            DocumentTypes dtCache = service.getDocumentTypes();
            DocumentType t = dtCache.getType("test:test");

            assertNotNull(t);

            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(t);
            System.err.println("test:test\n"+json);
        }
    }

    @Test
    public void testNodeTypeChangeListener() throws Exception {
        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        if (service != null) {
            EffectiveNodeTypes entCache1 = service.getEffectiveNodeTypes();
            DocumentTypes dtCache1 = service.getDocumentTypes();
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
            DocumentTypes dtCache2 = service.getDocumentTypes();
            if (dtCache1.version()==dtCache2.version()) {
                fail("DocumentTypes cache should have been reloaded.");
            }
            session.getRootNode().addNode("hippo:namespaces/test","hipposysedit:namespace");
            session.save();
            dtCache1 = service.getDocumentTypes();
            if (dtCache1.version()==dtCache2.version()) {
                fail("DocumentTypes cache should have been reloaded.");
            }
            if (dtCache1.getEffectiveNodeTypes().version() != dtCache2.getEffectiveNodeTypes().version()) {
                fail("EffectiveNodeTypes cache should not have been reloaded.");
            }
        }
    }
}
