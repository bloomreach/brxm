/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.hippoecm.repository.api.StringCodecFactory;

public class JcrCompactNodeTypeDefWriter {

    private static final String INDENT = "  ";
    private StringWriter out;
    private SortedSet<String> usedNamespaces = new TreeSet<String>();
    private final NodeTypeManager ntMgr;
    private final NamespaceRegistry nsReg;

    public JcrCompactNodeTypeDefWriter(NodeTypeManager ntMgr, NamespaceRegistry nsReg) {
        this.ntMgr = ntMgr;
        this.nsReg = nsReg;
    }

    private LinkedHashSet<NodeType> result;

    private Set<String> visited;

    public static String compactNodeTypeDef(Workspace workspace, String prefix) throws RepositoryException, IOException {
        JcrCompactNodeTypeDefWriter cndwriter = new JcrCompactNodeTypeDefWriter(workspace.getNodeTypeManager(), workspace.getNamespaceRegistry());
        return cndwriter.write(cndwriter.getNodeTypes(prefix));
    }
    
    synchronized NodeType[] getNodeTypes(String namespacePrefix) throws RepositoryException {
        NodeTypeIterator it = ntMgr.getAllNodeTypes();
        Set<NodeType> types = new TreeSet<NodeType>(new Comparator<NodeType>() {
            @Override public int compare(NodeType nt0, NodeType nt1) {
                return nt0.getName().compareTo(nt1.getName());
            }
        });
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            if (nt.getName().startsWith(namespacePrefix)) {
                types.add(nt);
            }
        }
        result = new LinkedHashSet<NodeType>();
        visited = new HashSet<String>();
        for (NodeType type : types) {
            visit(namespacePrefix, type);
        }
        NodeType[] returnValue = result.toArray(new NodeType[result.size()]);
        result = null;
        visited = null;
        return returnValue;
    }

    private void visit(String namespacePrefix, NodeType nt) {
        if (visited.contains(nt.getName())) {
            return;
        }
        visited.add(nt.getName());
        for (NodeType superType : nt.getSupertypes()) {
            visit(namespacePrefix, superType);
        }
        for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
            for (NodeType childType : nd.getRequiredPrimaryTypes()) {
                visit(namespacePrefix, childType);
            }
            NodeType defaultPriType = nd.getDefaultPrimaryType();
            if (defaultPriType != null) {
                visit(namespacePrefix, defaultPriType);
            }
        }
        if (nt.getName().startsWith(namespacePrefix+":")) {
            result.add(nt);
        }
    }

    public synchronized String write(NodeType[] types) throws RepositoryException, IOException {
        out = new StringWriter();
        for (NodeType nt : types) {
            writeName(nt);
            writeSupertypes(nt);
            writeOptions(nt);
            writePropDefs(nt);
            writeNodeDefs(nt);
            out.write("\n\n");
        }
        out.flush();
        String cnd = out.toString();
        if(nsReg != null) {
            out = new StringWriter();
            for(String prefix : usedNamespaces) {
                out.write("<'");
                out.write(prefix);
                out.write("'='");
                out.write(escape(nsReg.getURI(prefix)));
                out.write("'>\n");
            }
            out.flush();
            cnd = out.toString() + (usedNamespaces.size() > 0 ? "\n" : "") + cnd;
        }
        return cnd;
    }

    private void writeName(NodeType nt) throws IOException {
        out.write("[");
        out.write(resolve(nt.getName()));
        out.write("]");
    }

    private void writeSupertypes(NodeType nt) throws IOException {
        NodeType[] superTypes = nt.getDeclaredSupertypes();
        String delim = " > ";
        for (NodeType sn : superTypes) {
            out.write(delim);
            out.write(resolve(sn.getName()));
            delim = ", ";
        }
    }

    private void writeOptions(NodeType nt) throws IOException {
        if (nt.hasOrderableChildNodes()) {
            out.write("\n" + INDENT);
            out.write("orderable");
            if (nt.isMixin()) {
                out.write(" mixin");
            }
        } else if (nt.isMixin()) {
            out.write("\n" + INDENT);
            out.write("mixin");
        }
    }

    private void writePropDefs(NodeType nt) throws IOException {
        PropertyDefinition[] propdefs = nt.getDeclaredPropertyDefinitions();
        for (PropertyDefinition propdef : propdefs) {
            writePropDef(nt, propdef);
        }
    }

    private void writePropDef(NodeType nt, PropertyDefinition pd) throws IOException {
        out.write("\n" + INDENT + "- ");
        writeItemDefName(pd.getName());
        out.write(" (");
        out.write(PropertyType.nameFromValue(pd.getRequiredType()).toLowerCase());
        out.write(")");

        writeDefaultValues(pd.getDefaultValues());
        out.write(nt.getPrimaryItemName() != null && nt.getPrimaryItemName().equals(pd.getName()) ? " primary" : "");
        if (pd.isMandatory()) {
            out.write(" mandatory");
        }
        if (pd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (pd.isProtected()) {
            out.write(" protected");
        }
        if (pd.isMultiple()) {
            out.write(" multiple");
        }
        if (pd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(pd.getOnParentVersion()).toLowerCase());
        }
        writeValueConstraints(pd.getValueConstraints());
    }

    private void writeNodeDefs(NodeType nt) throws IOException {
        NodeDefinition[] childnodeDefs = nt.getDeclaredChildNodeDefinitions();
        for ( NodeDefinition childnodeDef : childnodeDefs) {
            writeNodeDef(nt, childnodeDef);
        }
    }

    private void writeNodeDef(NodeType nt, NodeDefinition nd) throws IOException {
        out.write("\n" + INDENT + "+ ");
        String name = nd.getName();
        if (name.equals("*")) {
            out.write('*');
        } else {
            writeItemDefName(name);
        }
        writeRequiredTypes(nd.getRequiredPrimaryTypes());
        writeDefaultType(nd.getDefaultPrimaryType());
        out.write(nt.getPrimaryItemName() != null && nt.getPrimaryItemName().equals(nd.getName()) ? " primary" : "");
        if (nd.isMandatory()) {
            out.write(" mandatory");
        }
        if (nd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (nd.isProtected()) {
            out.write(" protected");
        }
        if (nd.allowsSameNameSiblings()) {
            out.write(" multiple");
        }
        if (nd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(nd.getOnParentVersion()).toLowerCase());
        }
    }

    private void writeRequiredTypes(NodeType[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " (";
            for (NodeType reqType : reqTypes) {
                out.write(delim);
                out.write(resolve(reqType.getName()));
                delim = ", ";
            }
            out.write(")");
        }
    }

    private void writeDefaultType(NodeType defType) throws IOException {
        if (defType != null && !defType.getName().equals("*")) {
            out.write(" = ");
            out.write(resolve(defType.getName()));
        }
    }

    private void writeValueConstraints(String[] vca) throws IOException {
        if (vca != null && vca.length > 0) {
            String vc = vca[0];
            out.write(" < '");
            out.write(escape(vc));
            out.write("'");
            for (int i = 1; i < vca.length; i++) {
                vc = vca[i];
                out.write(", '");
                out.write(escape(vc));
                out.write("'");
            }
        }
    }

    private void writeItemDefName(String name) throws IOException {
        out.write(resolve(name));
    }

    private String resolve(String name) throws IOException {
        if (name == null) {
            return "";
        }

        if (name.contains(":")) {

            String prefix = name.substring(0, name.indexOf(":"));
            if (!"".equals(prefix)) {
                if (!usedNamespaces.contains(prefix)) {
                    usedNamespaces.add(prefix);
                }
                prefix += ":";
            }

            final String localName = name.substring(name.indexOf(":") + 1);
            String encLocalName = StringCodecFactory.ISO9075Helper.encodeLocalName(localName);
            String resolvedName = prefix + encLocalName;

            // check for '-' and '+'
            if (resolvedName.indexOf('-') >= 0 || resolvedName.indexOf('+') >= 0) {
                return "'" + resolvedName + "'";
            } else {
                return resolvedName;
            }
        } else {
            return name;
        }

    }

    private String escape(String s) {
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.insert(i, '\\');
                i++;
            } else if (sb.charAt(i) == '\'') {
                sb.insert(i, '\'');
                i++;
            }
        }
        return sb.toString();
    }

    private void writeDefaultValues(Value[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (Value element : dva) {
                out.write(delim);
                try {
                    out.write(escape(element.getString()));
                } catch (RepositoryException e) {
                    out.write(escape(element.toString()));
                }
                out.write("'");
                delim = ", '";
            }
        }
    }
}
