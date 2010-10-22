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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;

public class HierarchyResolverDecorator implements HierarchyResolver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Session session;
    HierarchyResolver hierarchyResolver;

    protected HierarchyResolverDecorator(SessionDecorator session, HierarchyResolver hierarchyResolver) {
        this.session = session;
        this.hierarchyResolver = hierarchyResolver;
    }

    protected void check() throws RepositoryException {
        if(!SessionDecorator.unwrap(session).isLive()) {
            this.hierarchyResolver = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver();
        }
    }

    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
        check();
        ancestor = NodeDecorator.unwrap(ancestor);
        return hierarchyResolver.getItem(ancestor, path, isProperty, last);
    }

    public Item getItem(Node node, String field) throws RepositoryException {
        check();
        node = NodeDecorator.unwrap(node);
        return hierarchyResolver.getItem(node, field);
    }

    public Property getProperty(Node node, String field) throws RepositoryException {
         check();
       node = NodeDecorator.unwrap(node);
        return hierarchyResolver.getProperty(node, field);
    }

    public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
        check();
        node = NodeDecorator.unwrap(node);
        return hierarchyResolver.getProperty(node, field);
    }

    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
        check();
        node = NodeDecorator.unwrap(node);
        return hierarchyResolver.getNode(node, field);
    }
}
