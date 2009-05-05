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
package org.hippoecm.repository.upgrade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.ext.UpdaterContext;

/*
 * Migration actions:
 * - change hippostd:fixeddirectory node type to hippostd:directory, as node type was dropped.
 */
public class M13 {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static class NamespaceMapping {
        String prefix;
        String oldVersion;
        String newVersion;
        String oldNamespaceURI;
        String newNamespaceURI;
        String cndName;
        InputStream cndStream;
        public NamespaceMapping(String prefix, String uri, String oldVersion, String newVersion, String cndName, InputStream cndStream) {
            this.prefix = prefix;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
            oldNamespaceURI = uri + oldVersion;
            newNamespaceURI = uri + newVersion;
            this.cndName = cndName;
            this.cndStream = cndStream;
        }
    }

    static private LinkedHashMap<String,NamespaceMapping> mappings;

    static void addMapping(NamespaceMapping mapping) {
        mappings.put(mapping.prefix, mapping);
    }

    Collection<NamespaceMapping> getNamespaceMappings() {
        return mappings.values();
    }

    static void initialize() {
        mappings = new LinkedHashMap<String,NamespaceMapping>();
        try {
            Class clazz = Class.forName("org.hippoecm.repository.LocalHippoRepository");
            addMapping(new NamespaceMapping("hippo",            "http://www.hippoecm.org/nt/",                "1.2", "1.3", "repository.cnd",       clazz.getResourceAsStream("repository.cnd")));
            addMapping(new NamespaceMapping("hippostd",         "http://www.hippoecm.org/hippostd/nt/",       "1.2", "1.3", "hippostd.cnd",         clazz.getResourceAsStream("hippostd-migration.cnd")));
            addMapping(new NamespaceMapping("hippolog",         "http://www.hippoecm.org/hippolog/nt/",       "1.2", "1.3", "logging.cnd",          clazz.getResourceAsStream("logging.cnd")));
            addMapping(new NamespaceMapping("frontend",         "http://www.hippoecm.org/frontend/nt/",       "1.3", "1.4", "frontend.cnd",         clazz.getResourceAsStream("frontend.cnd")));
            addMapping(new NamespaceMapping("reporting",        "http://www.hippoecm.org/reporting/nt/",      "1.3", "1.4", "reporting.cnd",        clazz.getResourceAsStream("reporting.cnd")));
            addMapping(new NamespaceMapping("defaultcontent",   "http://www.hippoecm.org/defaultcontent/nt/", "1.3", "1.4", "defaultcontent.cnd",   clazz.getClassLoader().getResourceAsStream("defaultcontent.cnd")));
            addMapping(new NamespaceMapping("hippoldap",        "http://www.hippoecm.org/hippoldap/nt/",      "1.2", "1.3", "hippoldap.cnd",        clazz.getResourceAsStream("hippoldap.cnd")));
            addMapping(new NamespaceMapping("hippogallery",     "http://www.hippoecm.org/hippogallery/nt/",   "1.2", "1.3", "hippogallery.cnd",     clazz.getClassLoader().getResourceAsStream("hippogallery.cnd")));
            addMapping(new NamespaceMapping("hippohtmlcleaner", "http://www.hippoecm.org/htmlcleaner/nt/",    "1.1", "1.2", "hippohtmlcleaner.cnd", clazz.getClassLoader().getResourceAsStream("hippohtmlcleaner.cnd")));
            addMapping(new NamespaceMapping("hipposched",       "http://www.hippoecm.org/hipposched/nt/",     "1.1", "1.2", "hipposched.cnd",       clazz.getClassLoader().getResourceAsStream("hipposched.cnd")));
        } catch (ClassNotFoundException ex) {
        }
    }

    public M13() {
    }

    protected void initializeDerivedNodeTypes(Workspace workspace) throws RepositoryException {
        CND cnd = new CND(workspace, new TreeSet<String>(mappings.keySet()));
        Set<String> addedPrefixes = cnd.addSubtypedNamespaces();
        for(String namespace : addedPrefixes) {
            ByteArrayOutputStream bastream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(bastream);
            Set<String> namespaceSet = new TreeSet<String>();
            namespaceSet.add(namespace);
            CND namespaceCND = new CND(workspace, namespaceSet) {
                @Override
                protected String getPrefixNamespaceURI(String prefix) throws RepositoryException {
                    String uri = super.getPrefixNamespaceURI(prefix);
                    if(M13.mappings.containsKey(prefix)) {
                        int pos = uri.lastIndexOf('.')+1;
                        uri = uri.substring(0, pos) + (Integer.parseInt(uri.substring(pos)) + 1);
                    }
                    return uri;
                }
            };
            namespaceCND.writeTo(writer);
            writer.flush();
            String namespaceURI = workspace.getNamespaceRegistry().getURI(namespace);
            String oldVersion = namespaceURI.substring(namespaceURI.lastIndexOf('/')+1);
            String newVersion = oldVersion.substring(0, oldVersion.lastIndexOf('.')+1) +
                (Integer.parseInt(oldVersion.substring(oldVersion.lastIndexOf('.')+1)) + 1);
            namespaceURI = namespaceURI.substring(0, namespaceURI.lastIndexOf('/')+1);
            addMapping(new NamespaceMapping(namespace, namespaceURI, oldVersion, newVersion, namespace + ".cnd", new ByteArrayInputStream(bastream.toByteArray())));
        }
    }

    protected static void loadNodeTypes(Workspace workspace, String cndName, InputStream cndStream) {
        try {
            CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName);
            List ntdList = cndReader.getNodeTypeDefs();
            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl)workspace.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

            for (Iterator iter = ntdList.iterator(); iter.hasNext(); ) {
                NodeTypeDef ntd = (NodeTypeDef)iter.next();

                try {
                    EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
                } catch (NamespaceException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage()); ex.printStackTrace();
                } catch (InvalidNodeTypeDefException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage()); ex.printStackTrace();
                } catch (RepositoryException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage()); ex.printStackTrace();
                }
            }
        } catch (ParseException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage()); ex.printStackTrace();
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage()); ex.printStackTrace();
        }
    }

    protected final String rename(String name) {
        int pos = name.indexOf(":");
        if(pos > 0) {
        NamespaceMapping mapping = mappings.get(name.substring(0, pos));
        if(mapping != null) {
            return mapping.prefix + "_" + mapping.newVersion + ":" + name.substring(name.indexOf(":") + 1);
        } else {
            return name;
        }
        } else {
            return name;
        }
    }

    protected final boolean isPrefix(String name) {
        int position = name.indexOf(":");
        if (position >= 0) {
            return mappings.containsKey(name.substring(0, position));
        } else {
            return false;
        }
    }

    private void dump(UpdaterContext context, Node node, int level) throws RepositoryException {
        dump(System.out, context, node, level);
    }

    private void dump(PrintStream out, UpdaterContext context, Node parent, int level) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        if (level > 0) {
            for (int i = 0; i < level - 1; i++) {
                sb.append("  ");
            }
            out.print(new String(sb));
            out.print("+ ");
            sb.append("  ");
        }
        String prefix = new String(sb);
        out.print((parent.getName().equals("") ? "/" : parent.getName()) + " [depth=" + parent.getDepth());
        if (parent.hasProperty("jcr:primaryType")) {
            out.print(",type=" + parent.getProperty("jcr:primaryType").getString());
        }
        if (parent.hasProperty("jcr:uuid")) {
            out.print(",uuid=" + parent.getProperty("jcr:uuid").getString());
        }
        out.println("]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            out.print(prefix + "- " + prop.getName() + " = ");
            if (context.isMultiple(prop)) {
                Value[] values = prop.getValues();
                out.print("{ ");
                for (int i = 0; i < values.length; i++) {
                    out.print(i > 0 ? ", " : "");
                    if (values[i].getType() == PropertyType.BINARY) {
                        out.print("<<binary>>");
                    } else {
                        out.print(values[i].getString());
                    }
                }
                out.println(" } ");
            } else {
                if (prop.getValue().getType() == PropertyType.BINARY) {
                    out.print("<<binary>>");
                } else {
                    out.println(prop.getString());
                }
            }
        }
        for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
                dump(out, context, node, level + 1);
            }
        }
    }

    protected void dump(UpdaterContext context, Node node) throws RepositoryException {
        dump(context, node, 0);
    }

    protected void dump(PrintStream ps, UpdaterContext context, Node node) throws RepositoryException {
        dump(ps, context, node, 0);
    }

    static class CND {
        Workspace workspace;
        Set<String> prefixes;
        LinkedHashSet<NodeType> nodeTypes;
        Set<String> namespaces;

        public CND(Workspace workspace, Set<String> prefixes) throws RepositoryException {
            this.workspace = workspace;
            this.prefixes = prefixes;
            nodeTypes = new LinkedHashSet<NodeType>();
            namespaces = new TreeSet<String>();
            namespaces.addAll(prefixes);
            SortedSet<NodeType> allNodeTypes = new TreeSet<NodeType>(new Comparator<NodeType>() {
                        public int compare(NodeType nt1, NodeType nt2) {
                            return nt1.getName().compareTo(nt2.getName());
                        }
                    });
            for (NodeTypeIterator ntiter = workspace.getNodeTypeManager().getAllNodeTypes(); ntiter.hasNext();) {
                NodeType nt = ntiter.nextNodeType();
                allNodeTypes.add(nt);
            }
            for (NodeType nt : allNodeTypes) {
                if (!nodeTypes.contains(nt)) {
                    processNodeType(nt);
                }
            }
        }

        public Set<String> addSubtypedNamespaces() throws RepositoryException {
            Set<String> addedPrefixes = new HashSet<String>();
            boolean prefixesSetChanged;
            do {
                prefixesSetChanged = false;
                for (NodeTypeIterator ntiter = workspace.getNodeTypeManager().getAllNodeTypes(); ntiter.hasNext();) {
                    NodeType nt = ntiter.nextNodeType();
                    String typePrefix = nt.getName();
                    if (typePrefix.contains(":")) {
                        typePrefix = typePrefix.substring(0, typePrefix.indexOf(":"));
                        NodeType[] superTypes = nt.getSupertypes();
                        for (NodeType superType : superTypes) {
                            String superTypePrefix = superType.getName();
                            if (superTypePrefix.contains(":")) {
                                superTypePrefix = superTypePrefix.substring(0, superTypePrefix.indexOf(":"));
                                if (prefixes.contains(superTypePrefix) && !prefixes.contains(typePrefix)) {
                                    addedPrefixes.add(typePrefix);
                                    prefixes.add(typePrefix);
                                    prefixesSetChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (prefixesSetChanged) {
                        break;
                    }
                }
            } while (prefixesSetChanged);
            return addedPrefixes;
        }

        private String processNodeType(NodeType nt) {
            String ntprefix = nt.getName();
            if (ntprefix.contains(":")) {
                ntprefix = ntprefix.substring(0, ntprefix.indexOf(":"));
                if(nodeTypes.contains(nt)) {
                    return ntprefix;
                }
                if (prefixes.contains(ntprefix)) {
                    nodeTypes.add(nt);
                    NodeType[] superNodeTypes = nt.getDeclaredSupertypes();
                    Arrays.sort(superNodeTypes, new Comparator<NodeType>() {
                        public int compare(NodeType nt1, NodeType nt2) {
                            return nt1.getName().compareTo(nt2.getName());
                        }
                    });
                    for (NodeType superNodeType : superNodeTypes) {
                        String superPrefix = processNodeType(superNodeType);
                        if (superPrefix != null) {
                            namespaces.add(superPrefix);
                        }
                    }
                    PropertyDefinition[] propertyDefinitions = nt.getDeclaredPropertyDefinitions();
                    Arrays.sort(propertyDefinitions, new Comparator<PropertyDefinition>() {
                        public int compare(PropertyDefinition pd1, PropertyDefinition pd2) {
                            if (pd2.getName().equals("*"))
                                return -1;
                            else
                                return pd1.getName().compareTo(pd2.getName());
                        }
                    });
                    for (PropertyDefinition propertyDefinition : propertyDefinitions) {
                        String propertyPrefix = propertyDefinition.getName();
                        if (propertyPrefix.contains(":")) {
                            propertyPrefix = propertyPrefix.substring(0, propertyPrefix.indexOf(":"));
                            namespaces.add(propertyPrefix);
                        }
                    }
                    NodeDefinition[] childNodeDefinitions = nt.getDeclaredChildNodeDefinitions();
                    Arrays.sort(childNodeDefinitions, new Comparator<NodeDefinition>() {
                        public int compare(NodeDefinition nd1, NodeDefinition nd2) {
                            if (nd2.getName().equals("*"))
                                return -1;
                            else
                                return nd1.getName().compareTo(nd2.getName());
                        }
                    });
                    for (NodeDefinition childNodeDefinition : childNodeDefinitions) {
                        if(childNodeDefinition.getDefaultPrimaryType() != null) {
                            String childPrefix = processNodeType(childNodeDefinition.getDefaultPrimaryType());
                            namespaces.add(childPrefix);
                        }
                        NodeType[] childNodeTypes = childNodeDefinition.getRequiredPrimaryTypes();
                        Arrays.sort(childNodeTypes, new Comparator<NodeType>() {
                            public int compare(NodeType nt1, NodeType nt2) {
                                return nt1.getName().compareTo(nt2.getName());
                            }
                        });
                        for (NodeType childNodeType : childNodeTypes) {
                            String childPrefix = processNodeType(childNodeType);
                            namespaces.add(childPrefix);
                        }
                    }
                    nodeTypes.remove(nt);
                    nodeTypes.add(nt);
                }
                return ntprefix;
            } else {
                return null;
            }
        }

        protected String getPrefixNamespaceURI(String prefix) throws RepositoryException {
            return workspace.getNamespaceRegistry().getURI(prefix);
        }

        public void writeTo(PrintWriter out) throws RepositoryException {
            for (String prefix : namespaces) {
                String uri = getPrefixNamespaceURI(prefix);
                out.print("<");
                out.print(prefix);
                out.print("='");
                out.print(escape(uri));
                out.println("'>");
            }
            for (NodeType nt : nodeTypes) {
                out.println();
                out.print("[");
                out.print(resolve(nt.getName()));
                out.print("]");

                NodeType[] superTypes = nt.getDeclaredSupertypes();
                String delim = " > ";
                for (NodeType sn : superTypes) {
                    if (!sn.isMixin()) {
                        out.print(delim);
                        out.print(resolve(sn.getName()));
                        delim = ", ";
                    }
                }
                for (NodeType sn : superTypes) {
                    if (sn.isMixin()) {
                        out.print(delim);
                        out.print(resolve(sn.getName()));
                        delim = ", ";
                    }
                }
                if (nt.hasOrderableChildNodes())
                    out.print(" orderable");
                if (nt.isMixin()) {
                    out.print(" mixin");
                }
                out.println();
                PropertyDefinition[] propdefs = nt.getDeclaredPropertyDefinitions();
                for (PropertyDefinition propdef : propdefs) {
                    out.print("- ");
                    out.print(resolve(propdef.getName()));
                    out.print(" (");
                    out.print(PropertyType.nameFromValue(propdef.getRequiredType()).toLowerCase());
                    out.print(")");
                    Value[] dva = propdef.getDefaultValues();
                    if (dva != null && dva.length > 0) {
                        delim = " = '";
                        for (Value element : dva) {
                            out.print(delim);
                            try {
                                out.print(escape(element.getString()));
                            } catch (RepositoryException ex) {
                                out.print(escape(element.toString()));
                            }
                            out.print("'");
                            delim = ", '";
                        }
                    }
                    out.print(nt.getPrimaryItemName() != null && nt.getPrimaryItemName().equals(propdef.getName()) ? " primary" : "");
                    if (propdef.isMandatory())
                        out.print(" mandatory");
                    if (propdef.isAutoCreated())
                        out.print(" autocreated");
                    if (propdef.isProtected())
                        out.print(" protected");
                    if (propdef.isMultiple())
                        out.print(" multiple");
                    if (propdef.getOnParentVersion() != OnParentVersionAction.COPY) {
                        out.print(" ");
                        out.print(OnParentVersionAction.nameFromValue(propdef.getOnParentVersion()).toLowerCase());
                    }
                    String[] vca = propdef.getValueConstraints();
                    if (vca != null && vca.length > 0) {
                        String vc = vca[0];
                        out.print(" < '");
                        out.print(escape(vc));
                        out.print("'");
                        for (int i = 1; i < vca.length; i++) {
                            vc = vca[i];
                            out.print(", '");
                            out.print(escape(vc));
                            out.print("'");
                        }
                    }
                    out.println();
                }
                NodeDefinition[] childnodeDefs = nt.getDeclaredChildNodeDefinitions();
                for (NodeDefinition nd : childnodeDefs) {
                    out.print("+ ");
                    String name = nd.getName();
                    if (name.equals("*")) {
                        out.print('*');
                    } else {
                        out.print(resolve(name));
                    }

                    NodeType[] reqTypes = nd.getRequiredPrimaryTypes();
                    if (reqTypes != null && reqTypes.length > 0) {
                        delim = " (";
                        for (NodeType reqType : reqTypes) {
                            out.print(delim);
                            out.print(resolve(reqType.getName()));
                            delim = ", ";
                        }
                        out.print(")");
                    }
                    NodeType defType = nd.getDefaultPrimaryType();
                    if (defType != null && !defType.getName().equals("*")) {
                        out.print(" = ");
                        out.print(resolve(defType.getName()));
                    }
                    out.print(nt.getPrimaryItemName() != null && nt.getPrimaryItemName().equals(nd.getName()) ? " primary" : "");
                    if (nd.isMandatory())
                        out.print(" mandatory");
                    if (nd.isAutoCreated())
                        out.print(" autocreated");
                    if (nd.isProtected())
                        out.print(" protected");
                    if (nd.allowsSameNameSiblings())
                        out.print(" multiple");
                    if (nd.getOnParentVersion() != OnParentVersionAction.COPY) {
                        out.print(" ");
                        out.print(OnParentVersionAction.nameFromValue(nd.getOnParentVersion()).toLowerCase());
                    }
                    out.println();
                }
            }
        }

        private String resolve(String name) {
            if (name == null) {
                return "";
            }

            if (name.indexOf(":") > -1) {

                String prefix = name.substring(0, name.indexOf(":"));
                if (!"".equals(prefix)) {
                    // check for writing namespaces
                    prefix += ":";
                }

                String encLocalName = NodeNameCodec.encode(name.substring(name.indexOf(":") + 1));
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
    }
}
