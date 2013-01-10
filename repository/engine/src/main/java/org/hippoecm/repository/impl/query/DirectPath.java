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
package org.hippoecm.repository.impl.query;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.impl.QueryDecorator;

public class DirectPath implements QueryDecorator.HardcodedQuery {

    public List<Node> execute(Session session, HippoQuery query, Map<String, Value> arguments)
            throws RepositoryException {
        String path = query.getStatement();
        boolean children = false;
        if (path.endsWith("/node()")) {
            path = path.substring(0, path.length() - "/node()".length());
            children = true;
        }
        if (path.startsWith("/jcr:root")) {
            path = path.substring("/jcr:root".length());
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Node root = session.getRootNode();
        Vector<Node> results = new Vector<Node>();

        Node node;
        if ("".equals(path)) {
            node = root;
        } else if (root.hasNode(path)) {
            node = root.getNode(path);
        } else {
            node = null;
        }

        if (node != null) {
            if (children) {
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    node = iter.nextNode();
                    if (node != null) {
                        results.add(node);
                    }
                }
            } else {
                results.add(node);
            }
        }
        return results;
    }
}
