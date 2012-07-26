/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.hippoecm.repository.api.HippoNodeType;

public class TypeResolverImpl implements TypeResolver {
    
    private Node types;

    public TypeResolverImpl(Node types) {
        this.types = types;
    }

    public String[] resolve(String className) throws RepositoryException {
        String primaryType = types.getNode(className).getProperty("hipposys:nodetype").getString();
        NodeType nodeType = types.getSession().getWorkspace().getNodeTypeManager().getNodeType(primaryType);
        if (nodeType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            return new String[] {primaryType, HippoNodeType.NT_HARDDOCUMENT};
        } else if (nodeType.isNodeType(HippoNodeType.NT_REQUEST)) {
            return new String[] {primaryType, "mix:referenceable"};
        } else {
            return new String[] {primaryType};
        }
    }
}
