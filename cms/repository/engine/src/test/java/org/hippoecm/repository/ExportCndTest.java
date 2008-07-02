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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.hippoecm.repository.decorating.SessionDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportCndTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCndExport() throws RepositoryException {
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator it = ntmgr.getAllNodeTypes();
        
        LinkedHashSet<NodeType> types = new LinkedHashSet<NodeType>();
        
        while(it.hasNext()) {
            NodeTypeImpl nt = (NodeTypeImpl)it.nextNodeType();
            if(nt.getName().startsWith("hippo:")) {
                types.add(nt);
            }
        }
        types = sortTypes(types);
        
        List<NodeTypeDef> nodeTypeDefs = new ArrayList<NodeTypeDef>();
        for(NodeType nt : types) {
            nodeTypeDefs.add( ((NodeTypeImpl)nt).getDefinition()) ;
        }
        
        NamespaceResolver nsRes = new SessionNamespaceResolver(session);
        Writer out = new StringWriter(); 
        try {
            CompactNodeTypeDefWriter.write(nodeTypeDefs, nsRes, (SessionImpl) SessionDecorator.unwrap(session), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        /*
         * this horrible line is because CompactNodeTypeDefWriter returns _x002a_ for something like:
         * - * (String)
         * 
         * For nodes, thus + * (nt:base) it works :S
         */
        
        String cnd = out.toString().replaceAll("_x002a_", "*");
        
    }

    private LinkedHashSet<NodeType> sortTypes(LinkedHashSet<NodeType> types) {
        return new SortContext(types).sort();
    }

    class SortContext {
        HashSet<NodeType> visited;
        LinkedHashSet<NodeType> result;
        LinkedHashSet<NodeType> set;

        SortContext(LinkedHashSet<NodeType> set) {
            this.set = set;
            visited = new HashSet<NodeType>();
            result = new LinkedHashSet<NodeType>();
        }

        void visit(NodeType nt) {
            if (visited.contains(nt) || !set.contains(nt)) {
                return;
            }

            visited.add(nt);
            for (NodeType superType : nt.getSupertypes()) {
                visit(superType);
            }
            for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
                visit(nd.getDeclaringNodeType());
            }
            result.add(nt);
        }

        LinkedHashSet<NodeType> sort() {
            for (NodeType type : set) {
                visit(type);
            }
            return result;
        }
    }
}
