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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class ExportCndTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testJRCndExport() throws RepositoryException {
        LinkedHashSet<NodeType> types = getSortedNodeTypes("hippo:");

        List<QNodeTypeDefinition> nodeTypeDefs = new ArrayList<QNodeTypeDefinition>();
        for (NodeType nt : types) {
            nodeTypeDefs.add(((NodeTypeImpl) nt).getDefinition());
        }

        NamespaceResolver nsRes = new SessionNamespaceResolver(session);
        Writer out = new StringWriter();
        try {
            Session impl = session;
            impl = org.hippoecm.repository.decorating.SessionDecorator.unwrap(impl);
            CompactNodeTypeDefWriter.write(nodeTypeDefs, nsRes, (SessionImpl)impl, out);
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

    @Test
    public void testJCROnlyCndExport() throws RepositoryException {
        LinkedHashSet<NodeType> types = getSortedNodeTypes("hippo:");

        Writer out = new StringWriter();

        try {
            out = new HippoCompactNodeTypeDefWriter(session).write(types, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * this horrible line is because CompactNodeTypeDefWriter returns _x002a_ for something like:
         * - * (String)
         *
         * For nodes, thus + * (nt:base) it works :S
         */
        String cnd = out.toString();

    }

    class HippoCompactNodeTypeDefWriter {

        private static final String INDENT = "  ";
        private Writer out;
        private Session session;
        private Writer nsWriter ;
        private Set<String> usedNamespaces = new HashSet<String>();

        public HippoCompactNodeTypeDefWriter(Session session) {
            this.session = session;
        }

        public Writer write(LinkedHashSet<NodeType> types, boolean includeNS) throws IOException {
            this.out = new StringWriter();;
            if(includeNS) {
                nsWriter  = new StringWriter();
            }
            for (NodeType nt : types) {
                writeName(nt);
                writeSupertypes(nt);
                writeOptions(nt);
                writePropDefs(nt);
                writeNodeDefs(nt);
                this.out.write("\n\n");
            }
            if (nsWriter != null) {
                nsWriter.write("\n");
                this.out.close();
                nsWriter.write(((StringWriter) out).getBuffer().toString());
                this.out = nsWriter;
                nsWriter = null;
            }
            this.out.flush();
            return out;
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
                for (int i = 0; i < reqTypes.length; i++) {
                    out.write(delim);
                    out.write(resolve(reqTypes[i].getName()));
                    delim = ", ";
                }
                out.write(")");
            }
        }

        /**
         * write default types
         * @param defType
         */
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

            if (name.indexOf(":") > -1) {

                String prefix = name.substring(0, name.indexOf(":"));
                if (!"".equals(prefix)) {
                    // check for writing namespaces
                    if (nsWriter != null) {
                        if (!usedNamespaces.contains(prefix)) {
                            usedNamespaces.add(prefix);
                            nsWriter.write("<'");
                            nsWriter.write(prefix);
                            nsWriter.write("'='");
                            // TODO write namespace
                            try {
                                nsWriter.write(escape(session.getNamespaceURI(prefix)));
                            } catch (NamespaceException e) {
                                e.printStackTrace();
                            } catch (RepositoryException e) {
                                e.printStackTrace();
                            }
                            nsWriter.write("'>\n");
                        }
                    }
                    prefix += ":";
                }

                final String localname = name.substring(name.indexOf(":") + 1);
                String encLocalName = StringCodecFactory.ISO9075Helper.encodeLocalName(localname);
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
            StringBuffer sb = new StringBuffer(s);
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
                for (int i = 0; i < dva.length; i++) {
                    out.write(delim);
                    try {
                        out.write(escape(dva[i].getString()));
                    } catch (RepositoryException e) {
                        out.write(escape(dva[i].toString()));
                    }
                    out.write("'");
                    delim = ", '";
                }
            }
        }
    }

    private LinkedHashSet<NodeType> getSortedNodeTypes(String namespacePrefix) throws RepositoryException {
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator it = ntmgr.getAllNodeTypes();

        LinkedHashSet<NodeType> types = new LinkedHashSet<NodeType>();

        while (it.hasNext()) {
            NodeType nt = (NodeType) it.nextNodeType();
            if (nt.getName().startsWith(namespacePrefix)) {
                types.add(nt);
            }
        }
        types = sortTypes(types);
        return types;
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
