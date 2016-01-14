/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ResourceContext;

public interface Visitor {
    VisitorFactory getVisitorFactory();
    void visit(ResourceContext context, Property property, final Map<String, Object> destination) throws RepositoryException;
    void visit(ResourceContext context, Node node, final Map<String, Object> destination) throws RepositoryException;
    void visit(ResourceContext context, PropertyIterator propertyIterator, final Map<String, Object> destination) throws RepositoryException;
    void visit(ResourceContext context, NodeIterator nodeIterator, final Map<String, Object> destination) throws RepositoryException;
}