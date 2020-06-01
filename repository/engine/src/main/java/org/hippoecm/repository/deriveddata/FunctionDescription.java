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
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;

class FunctionDescription {

    private final Node functionNode;

    FunctionDescription(final Node function) {
        this.functionNode = function;
    }

    String getName() throws RepositoryException {
        return functionNode.getName();
    }

    String getClassName() throws RepositoryException {
        return functionNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
    }

    String getApplicableNodeType() throws RepositoryException {
        return functionNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();
    }

    Collection<PropertyReference> getAccessedProperties() throws RepositoryException {
        final Set<PropertyReference> references = new HashSet<PropertyReference>();
        for (Node propDef : new NodeIterable(functionNode.getNode("hipposys:accessed").getNodes())) {
            if (propDef == null) {
                DerivedDataEngine.log.error("unable to access derived data accessed property definition");
                continue;
            }
            references.add(PropertyReference.createPropertyReference(propDef, this));
        }
        return references;
    }

    Collection<PropertyReference> getDerivedProperties() throws RepositoryException {
        final Set<PropertyReference> references = new HashSet<PropertyReference>();
        for (Node propDef : new NodeIterable(functionNode.getNode(HippoNodeType.HIPPO_DERIVED).getNodes())) {
            if (propDef == null) {
                DerivedDataEngine.log.error("unable to access derived data derived property definition");
                continue;
            }
            references.add(PropertyReference.createPropertyReference(propDef, this));
        }
        return references;
    }

}
