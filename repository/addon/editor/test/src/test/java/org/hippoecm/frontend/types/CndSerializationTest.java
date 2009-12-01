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
package org.hippoecm.frontend.types;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.wicket.Session;
import org.hippoecm.editor.cnd.CndSerializer;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.session.UserSession;
import org.junit.Test;

public class CndSerializationTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testSerialization() throws Exception {
        JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();
        CndSerializer serializer = new CndSerializer(sessionModel, "test");
        String cnd = serializer.getOutput();

        CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new StringReader(cnd), "test");
        NamespaceMapping mapping = cndReader.getNamespaceMapping();
        assertEquals("http://www.hippoecm.org/editor/test/nt/0.2", mapping.getURI("test"));
        assertTrue(mapping.getPrefixToURIMapping().containsKey("nt"));
        
        Map<String, NodeTypeDef> nodeTypes = new TreeMap<String, NodeTypeDef>();
        for (NodeTypeDef def : (List<NodeTypeDef>) cndReader.getNodeTypeDefs()) {
            nodeTypes.put(def.getName().getLocalName(), def);
        }

        // test:test
        {
            NodeTypeDef ntd = nodeTypes.get("test");
            NodeDef childNodes[] = ntd.getChildNodeDefs();
            assertEquals(1, childNodes.length);
    
            Name[] priTypes = childNodes[0].getRequiredPrimaryTypes();
            assertEquals(1, priTypes.length);
            assertEquals("nt", mapping.getPrefix(priTypes[0].getNamespaceURI()));
            assertEquals("unstructured", priTypes[0].getLocalName());
    
            PropDef[] pds = ntd.getPropertyDefs();
            assertEquals(1, pds.length);
    
            assertEquals("title", pds[0].getName().getLocalName());
            assertEquals(1, pds[0].getRequiredType());
        }

        // test:inheriting
        {
            NodeTypeDef ntd = nodeTypes.get("inheriting");
            PropDef[] pds = ntd.getPropertyDefs();
            assertEquals(1, pds.length);
    
            assertEquals("extra", pds[0].getName().getLocalName());
            assertEquals(1, pds[0].getRequiredType());
        }
    }

}
