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
package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;

public interface ColumnResolver {
    
    public PropertyDefinition resolvePropertyDefinition(Node node, String column, int propertyType) throws RepositoryException;
    
    public Property resolveProperty(Node node, String column) throws RepositoryException;

    public Node resolveNode(Node node, String column) throws RepositoryException;

    public ColumnResolver.NodeLocation resolveNodeLocation(Node node, String column) throws RepositoryException;

    public JcrOID resolveClone(Cloneable cloned) throws RepositoryException;

    public Node copyClone(Node source, Cloneable cloned, Node target, String name, Node current) throws RepositoryException;

    public class NodeLocation {
        Node parent;
        Node child;
        String name;

        public NodeLocation(Node parent, Node child, String name) {
            this.parent = parent;
            this.child = child;
            this.name = name;
        }
    }
}
