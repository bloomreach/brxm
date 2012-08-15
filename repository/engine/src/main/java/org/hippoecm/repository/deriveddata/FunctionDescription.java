/*
 *  Copyright 2012 Hippo.
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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FunctionDescription {

    static final Logger log = LoggerFactory.getLogger(FunctionDescription.class);

    private final Node function;

    FunctionDescription(final Node function) {
        this.function = function;
    }

    String getClassName() throws RepositoryException {
        return function.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
    }

    String getNodeTypeName() throws RepositoryException {
        return function.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();
    }

    Set<PropertyReference> getAccessedProperties() throws RepositoryException {
        Set<PropertyReference> references = new HashSet<PropertyReference>();
        for (Node propDef : new NodeIterable(function.getNode("hipposys:accessed").getNodes())) {
            if (propDef == null) {
                DerivedDataEngine.logger.error("unable to access derived data accessed property definition");
                continue;
            }

            references.add(new PropertyReference(propDef));
        }
        return references;
    }

    Set<PropertyReference> getDerivedProperties() throws RepositoryException {
        Set<PropertyReference> references = new HashSet<PropertyReference>();
        for (Node propDef : new NodeIterable(function.getNode(
                HippoNodeType.HIPPO_DERIVED).getNodes())) {
            if (propDef == null) {
                DerivedDataEngine.logger.error("unable to access derived data derived property definition");
                continue;
            }

            references.add(new PropertyReference(propDef));
        }
        return references;
    }

    @Override
    public String toString() {
        try {
            return function.getPath();
        } catch (RepositoryException e) {
            log.error("Unable to retrieve path for function node", e);
            return super.toString();
        }
    }
}
