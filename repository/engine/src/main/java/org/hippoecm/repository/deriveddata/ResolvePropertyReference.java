/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.deriveddata;

import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;

public class ResolvePropertyReference extends PropertyReference {

    public ResolvePropertyReference(final Node node, final FunctionDescription function) {
        super(node, function);
    }

    @Override
    Value[] getPropertyValues(Node modified, Collection<String> dependencies) throws RepositoryException {
        /* FIXME: should read:
        * Property property = ((HippoWorkspace)(modified.getSession().getWorkspace())).getHierarchyResolver().getProperty(modified, propDef.getProperty("hipposys:relPath").getString());
        * however this is broken because of a cast exception as the session is not wrapped
        */
        HierarchyResolver.Entry lastNode = new HierarchyResolver.Entry();
        Property property = new HierarchyResolverImpl().getProperty(modified, getRelativePath(), lastNode);
        if (property != null) {
            if (property.getParent().isNodeType("mix:referenceable")) {
                dependencies.add(property.getParent().getIdentifier());
            }
            if (!property.getDefinition().isMultiple()) {
                return new Value[] { property.getValue() };
            } else {
                return property.getValues();
            }
        } else {
            if (lastNode.node.isNodeType("mix:referenceable")) {
                dependencies.add(lastNode.node.getIdentifier());
            }
        }
        return null;
    }

    @Override
    boolean persistPropertyValues(final Node modified, final Map<String, Value[]> parameters) throws RepositoryException {
        return false;
    }

    private String getRelativePath() throws RepositoryException {
        return node.getProperty("hipposys:relPath").getString();
    }

}
