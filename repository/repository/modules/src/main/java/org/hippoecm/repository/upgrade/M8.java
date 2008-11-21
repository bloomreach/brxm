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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;

import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;

import org.hippoecm.repository.ext.UpdaterContext;

public class M8 {
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
        public NamespaceMapping(String prefix, String uri, String cndName, InputStream cndStream) {
            this.prefix = prefix;
            oldVersion = "1.0";
            newVersion = "1.1";
            oldNamespaceURI = uri + oldVersion;
            newNamespaceURI = uri + newVersion;
            this.cndName = cndName;
            this.cndStream = cndStream;
        }
    }

    List<NamespaceMapping> mappings;
    Set<String> prefixes;

    public M8() {
        mappings = new LinkedList<NamespaceMapping>();
        prefixes = new HashSet<String>();
        try {
            Class clazz = Class.forName("org.hippoecm.repository.LocalHippoRepository");
            mappings.add(new NamespaceMapping("hippo",          "http://www.hippoecm.org/nt/",                "repository.cnd",     clazz.getResourceAsStream("repository.cnd")));
            mappings.add(new NamespaceMapping("hippostd",       "http://www.hippoecm.org/hippostd/nt/",       "hippostd.cnd",       clazz.getResourceAsStream("hippostd.cnd")));
            mappings.add(new NamespaceMapping("reporting",      "http://www.hippoecm.org/reporting/nt/",      "reporting.cnd",      clazz.getResourceAsStream("reporting.cnd")));
            mappings.add(new NamespaceMapping("hippolog",       "http://www.hippoecm.org/hippolog/nt/",       "logging.cnd",        clazz.getResourceAsStream("logging.cnd")));
            mappings.add(new NamespaceMapping("frontend",       "http://www.hippoecm.org/frontend/nt/",       "frontend.cnd",       clazz.getResourceAsStream("frontend.cnd")));
            mappings.add(new NamespaceMapping("defaultcontent", "http://www.hippoecm.org/defaultcontent/nt/", "defaultcontent.cnd", clazz.getResourceAsStream("defaultcontent.cnd")));
            mappings.add(new NamespaceMapping("hippoldap",      "http://www.hippoecm.org/hippoldap/nt/",      "hippoldap.cnd",      clazz.getResourceAsStream("hippoldap.cnd")));
            mappings.add(new NamespaceMapping("hippogallery",   "http://www.hippoecm.org/hippogallery/nt/",   "gippogallery.cnd",   clazz.getClassLoader().getResourceAsStream("hippogallery.cnd")));
            for(NamespaceMapping mapping : mappings) {
                prefixes.add(mapping.prefix);
            }
        } catch(ClassNotFoundException ex) {
        }
    }

            protected final String rename(String name) {
                return name.substring(0,name.indexOf(":")) + "_1.1:" + name.substring(name.indexOf(":")+1);
            }
            protected final boolean isPrefix(String name) {
                int position = name.indexOf(":");
                if(position >= 0) {
                    return prefixes.contains(name.substring(0,position));
                } else {
                    return false;
                }
            }

    private void dump(UpdaterContext context, Node node, int level) throws RepositoryException {
        dump(System.out, context, node, level);
    }

    private void dump(PrintStream out, UpdaterContext context, Node parent, int level) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        if(level > 0) {
            for (int i = 0; i < level-1; i++) {
                sb.append("  ");
            }
            out.print(new String(sb));
            out.print("+ ");
            sb.append("  ");
        }
        String prefix = new String(sb);
        // out.print(parent.getPath() + " [name=" + parent.getName() + ",depth=" + parent.getDepth());
        out.print((parent.getName().equals("")?"/":parent.getName()) + " [depth=" + parent.getDepth());
        if (parent.hasProperty("jcr:primaryType")) {
            out.print(",type="+parent.getProperty("jcr:primaryType").getString());
        }
        if (parent.hasProperty("jcr:uuid")) {
            out.print(",uuid="+parent.getProperty("jcr:uuid").getString());
            // out.print(",uuid=...");
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
        for (NodeIterator iter = parent.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
                dump(out, context, node, level + 1);
            }
        }
    }

    public void dump(UpdaterContext context, Node node) throws RepositoryException {
        dump(context, node, 0);
    }

    public void dump(PrintStream ps, UpdaterContext context, Node node) throws RepositoryException {
        dump(ps, context, node, 0);
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
                } catch (InvalidNodeTypeDefException ex) {
                } catch (RepositoryException ex) {
                }
            }
        } catch (ParseException ex) {
        } catch (RepositoryException ex) {
        }
    }
}
