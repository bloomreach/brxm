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
package org.hippoecm.repository.api;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * This interface is not yet part of the public API of the Hippo Repository.
 */
public interface HierarchyResolver {

    public final class Entry {
        public Node node;
        public String relPath;
    }

    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException;

    public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException;

    public Property getProperty(Node node, String field) throws RepositoryException;

    public Property getProperty(Node node, String field, Entry last) throws RepositoryException;

    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException;

}
