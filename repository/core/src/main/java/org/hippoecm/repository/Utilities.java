/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.PrintStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class Utilities {
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
            // out.print(",uuid="+parent.getProperty("jcr:uuid").getString());
            out.print(",uuid=...");
        }
        out.println("]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if(prop.getName().equals("jcr:primaryType") ||
               prop.getName().equals("jcr:uuid") ||
               prop.getName().equals("hippo:paths")) {
                continue;
            }
            out.print(prefix + "- " + prop.getName() + " = ");
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                out.print("{ ");
                for (int i = 0; i < values.length; i++) {
                    out.print((i > 0 ? ", " : "") + values[i].getString());
                }
                out.println(" } ");
            } else {
                out.println(prop.getString());
            }
        }
        for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
                dump(out, node, level + 1);
            }
        }
    }

    public static void dump(Node node) throws RepositoryException {
        dump(node, 0);
    }

    public static void dump(PrintStream ps, Node node) throws RepositoryException {
        dump(ps, node, 0);
    }
}
