/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class GetRandomChildNodeAction extends Action {

    private Random random = new Random(System.currentTimeMillis());
    
    public GetRandomChildNodeAction(ActionContext context) {
        super(context);
    }
    
    @Override
    public boolean canOperateOnNode(Node node) throws RepositoryException {
        return (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) && node.hasNodes();
    }

    @Override
    public Node doExecute(Node node) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        int size = (int)nodes.getSize();
        if (size > 0) {
            int index = random.nextInt(size);
            if (index > 0) {
                nodes.skip(index-1);
            }
            Node child = nodes.nextNode();
            if (child.isNodeType("hippo:handle")) {
                child = child.getNode(child.getName());
            }
            node.getSession().refresh(true);
            return child;
        }
        return null;
    }

    @Override
    public double getWeight() {
        return 2.0;
    }

    @Override
    public boolean isWriteAction() {
        return false;
    }

}
