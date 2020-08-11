/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.RegistryNamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeType;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;

public class JcrCompactNodeTypeDefWriter {

    private final NodeTypeManager ntMgr;
    private final NamespaceRegistry nsReg;

    public JcrCompactNodeTypeDefWriter(NodeTypeManager ntMgr, NamespaceRegistry nsReg) {
        this.ntMgr = ntMgr;
        this.nsReg = nsReg;
    }

    public static String compactNodeTypeDef(Workspace workspace, String prefix) throws RepositoryException, IOException {
        JcrCompactNodeTypeDefWriter cndwriter = new JcrCompactNodeTypeDefWriter(workspace.getNodeTypeManager(), workspace.getNamespaceRegistry());
        return cndwriter.write(cndwriter.getNodeTypes(prefix));
    }

    private synchronized LinkedHashSet<QNodeTypeDefinition> getNodeTypes(String namespacePrefix) throws RepositoryException {
        NodeTypeIterator it = ntMgr.getAllNodeTypes();
        TreeMap<String, NodeType> typesMap = new TreeMap<>();
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            if (nt.getName().startsWith(namespacePrefix)) {
                typesMap.put(nt.getName(), nt);
            }
        }
        LinkedHashSet<QNodeTypeDefinition> result = new LinkedHashSet<>();
        for (NodeType type : typesMap.values()) {
            visit(namespacePrefix, type, new HashSet<>(), result);
        }
        return result;
    }

    private void visit(String namespacePrefix, NodeType nt, HashSet<String> visited, LinkedHashSet<QNodeTypeDefinition> result) {
        if (visited.contains(nt.getName())) {
            return;
        }
        visited.add(nt.getName());
        for (NodeType superType : nt.getSupertypes()) {
            visit(namespacePrefix, superType, visited, result);
        }
        for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
            for (NodeType childType : nd.getRequiredPrimaryTypes()) {
                visit(namespacePrefix, childType, visited, result);
            }
            NodeType defaultPriType = nd.getDefaultPrimaryType();
            if (defaultPriType != null) {
                visit(namespacePrefix, defaultPriType, visited, result);
            }
        }
        if (nt.getName().startsWith(namespacePrefix + ":")) {
            result.add(((AbstractNodeType)nt).getDefinition());
        }
    }

    public synchronized String write(NodeType[] types) throws RepositoryException, IOException {
        LinkedHashSet<QNodeTypeDefinition> ntdefs = new LinkedHashSet<>();
        for (NodeType nt : types) {
            ntdefs.add(((AbstractNodeType)nt).getDefinition());
        }
        return write(ntdefs);
    }

    public synchronized String write(Collection<QNodeTypeDefinition> types) throws RepositoryException, IOException {
        StringWriter out = new StringWriter();
        CompactNodeTypeDefWriter.write(types, new RegistryNamespaceResolver(nsReg), new DefaultNamePathResolver(nsReg), out);
        return out.toString();
    }
}
