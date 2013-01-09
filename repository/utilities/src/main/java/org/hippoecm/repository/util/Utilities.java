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
package org.hippoecm.repository.util;

import java.io.PrintStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    private static final Logger log = LoggerFactory.getLogger(Utilities.class);

    private static void dump(Node node, int level) throws RepositoryException {
        dump(System.out, node, level);
    }

    private static void dump(PrintStream out, Node parent, int level) throws RepositoryException {
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
        out.print(",path="+parent.getPath());
        out.print(",id="+parent.getIdentifier());
        out.println("]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if(prop.getName().equals("jcr:primaryType") ||
               prop.getName().equals("jcr:uuid") ||
               prop.getName().equals(HippoNodeType.HIPPO_PATHS)) {
                continue;
            }
            out.print(prefix + "- " + prop.getName() + " = ");
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                out.print("{ ");
                for (int i = 0; i < values.length; i++) {
                    out.print(i > 0 ? ", " : "");
                    if (values[i].getType() == PropertyType.BINARY || prop.getName().equals("jcr:data")) {
                        out.print("<<binary>>");
                    } else if(values[i].getString().contains("\n")) {
                        out.println("<<multi-line>>");
                    } else {
                        out.print(values[i].getString());
                    }
                }
                out.println(" } ");
            } else {
                if (prop.getType() == PropertyType.BINARY || prop.getName().equals("jcr:data")) {
                    out.println("<<binary>>");
                } else {
                    if(prop.getString().contains("\n")) {
                        out.println("<<multi-line>>");
                    } else {
                        out.println(prop.getString());
                    }
                }
            }
        }
        for (NodeIterator iter = parent.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system") && (!(node instanceof HippoNode) || ( ((HippoNode)node).getCanonicalNode() != null && node.isSame(((HippoNode)node).getCanonicalNode())))) {
                dump(out, node, level + 1);
            }
        }
    }

    /** Method used to display the node and all its children in a human
     * interpretable representation to <code>System.out</code>
     *
     * @param node the node from which to start printing the tree
     */
    public static void dump(Node node) throws RepositoryException {
        dump(node, 0);
    }

    /** Method used to display the node and all its children in a human
     * interpretable representation to the indicated <code>PrintStream</code>.
     *
     * @param ps the printstream to which to output
     * @param node the node from which to start printing the tree
     */
    public static void dump(PrintStream ps, Node node) throws RepositoryException {
        dump(ps, node, 0);
    }
}
