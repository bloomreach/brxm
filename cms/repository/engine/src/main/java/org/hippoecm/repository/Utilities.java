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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.servicing.ServicingNodeImpl;

public class Utilities {
    private final static Logger log = LoggerFactory.getLogger(Utilities.class);

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
        for (NodeIterator iter = parent.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
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

    /**
     * Deprecated from the start, this convenience method is used to look up
     * a node from a starting ancestor with a relative path, which may contain
     * an XPATH expression.
     *
     * @param parent the ancestor relative to which the path is expressed
     * @param relPath the relative XPATH
     * @returns the first node matching the expression or <code>null</code> if
     *          no match was found
     * @deprecated
     */
    public static Node getNode(Node parent, String relPath) throws RepositoryException {
        String statement = parent.getPath() + "/" + relPath;
        while(statement.startsWith("/"))
            statement = statement.substring(1);
        if(log.isDebugEnabled())
            log.debug("getNode "+statement);
        Query query = parent.getSession().getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        return iter.hasNext() ? iter.nextNode() : null;
    }

    /**
     * Convenience function to copy a node to a destination path in the same workspace
     *
     * @param srcNode the source path node to copy
     * @param destAbsPath the absolute path where the srcNode contents should be copied to
     * @returns the resulting copy
     */
    public static Node copy(Node srcNode, String destAbsPath) throws PathNotFoundException, ItemExistsException,
      LockException, VersionException, RepositoryException {
        while (destAbsPath.startsWith("/")) {
            destAbsPath = destAbsPath.substring(1);
        }
        Node destNode = srcNode.getSession().getRootNode();
        int p = destAbsPath.lastIndexOf("/");
        if(p > 0) {
            destNode = destNode.getNode(destAbsPath.substring(0,p));
            destAbsPath = destAbsPath.substring(p+1);
        }
        try {
            destNode = destNode.addNode(destAbsPath, srcNode.getPrimaryNodeType().getName());
            copy(srcNode, destNode);
            return destNode;
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }

    private static void copy(Node srcNode, Node destNode) throws ItemExistsException, LockException, RepositoryException {
        try {
            srcNode = ServicingNodeImpl.unwrap(srcNode);
            for(PropertyIterator iter = srcNode.getProperties(); iter.hasNext(); ) {
                Property property = iter.nextProperty();
                if(!property.getName().equals("jcr:primaryType") && !property.getName().equals("jcr:uuid")) {
                    if(property.getDefinition().isMultiple())
                        destNode.setProperty(property.getName(), property.getValues());
                    else
                        destNode.setProperty(property.getName(), property.getValue());
                }
            }
            for(NodeIterator iter = srcNode.getNodes(); iter.hasNext(); ) {
                Node node = iter.nextNode();
                Node child = destNode.addNode(node.getName(), srcNode.getPrimaryNodeType().getName());
                copy(node, child);
            }
        } catch(PathNotFoundException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(VersionException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(ValueFormatException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch(NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }
}
