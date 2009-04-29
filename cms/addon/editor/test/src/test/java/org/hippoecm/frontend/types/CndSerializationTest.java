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

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.hippoecm.editor.tools.CndSerializer;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.Test;

public class CndSerializationTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    HippoTester tester;
    Home home;
    IPluginContext context;
    JcrSessionModel sessionModel;

    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        sessionModel = new JcrSessionModel(Main.DEFAULT_CREDENTIALS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }
        };
        tester = new HippoTester(sessionModel);
        home = (Home) tester.startPage(Home.class);
        context = new PluginContext(home.getPluginManager(), new JavaPluginConfig("test"));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSerialization() throws Exception {
        CndSerializer serializer = new CndSerializer(context, sessionModel, "test");
        String cnd = serializer.getOutput();

        CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new StringReader(cnd), "test");
        NamespaceMapping mapping = cndReader.getNamespaceMapping();
        assertEquals("http://www.hippoecm.org/frontend/test/nt/0.2", mapping.getURI("test"));
        assertTrue(mapping.getPrefixToURIMapping().containsKey("nt"));
        
        List<NodeTypeDef> ntDefs = cndReader.getNodeTypeDefs();
        assertEquals(1, ntDefs.size());
        
        NodeTypeDef ntd = ntDefs.get(0);
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

}
