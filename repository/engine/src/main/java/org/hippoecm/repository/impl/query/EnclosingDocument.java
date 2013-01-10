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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.impl.QueryDecorator;

public class EnclosingDocument implements QueryDecorator.HardcodedQuery {

    public List<Node> execute(Session session, HippoQuery query, Map<String,Value> arguments) throws RepositoryException {
        Vector results = new Vector<Node>();
        String uuid = arguments.get("id").getString();
        try {
            Node node = session.getNodeByUUID(uuid);
            for (int depth = node.getDepth() - 1; depth >= 0; depth--) {
                try {
                    Node ancestor = (Node)node.getAncestor(depth);
                    if (ancestor.isNodeType("hippo:document")) {
                        results.add(ancestor);
                    }
                } catch (ItemNotFoundException ex) {
                } catch (AccessDeniedException ex) {
                }
            }
        } catch (ItemNotFoundException ex) {
        }
        return results;
    }
}
