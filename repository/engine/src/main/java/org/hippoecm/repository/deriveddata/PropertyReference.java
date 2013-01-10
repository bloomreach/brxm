/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.deriveddata;

import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

abstract class PropertyReference {

    protected final Node node;
    protected final FunctionDescription function;

    protected PropertyReference(final Node propertyReference, final FunctionDescription function) {
        this.node = propertyReference;
        this.function = function;
    }

    protected ValueFactory getValueFactory() throws RepositoryException {
        return node.getSession().getValueFactory();
    }

    static PropertyReference createPropertyReference(final Node node, final FunctionDescription function) throws RepositoryException {
        if (node.isNodeType("hipposys:builtinpropertyreference")) {
            return new BuiltinPropertyReference(node, function);
        } else if (node.isNodeType("hipposys:relativepropertyreference")) {
            return new RelativePropertyReference(node, function);
        } else if (node.isNodeType("hipposys:resolvepropertyreference")) {
            return new ResolvePropertyReference(node, function);
        }
        return null;
    }

    String getName() throws RepositoryException {
        return node.getName();
    }

    abstract Value[] getPropertyValues(Node modified, Collection<String> dependencies) throws RepositoryException;

    abstract boolean persistPropertyValues(final Node modified, final Map<String, Value[]> parameters) throws RepositoryException;

}
