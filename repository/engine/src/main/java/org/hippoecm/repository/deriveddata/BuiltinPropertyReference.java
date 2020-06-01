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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

class BuiltinPropertyReference extends PropertyReference {

    protected BuiltinPropertyReference(final Node node, final FunctionDescription function) {
        super(node, function);
    }

    @Override
    Value[] getPropertyValues(Node modified, Collection<String> dependencies) throws RepositoryException {
        if (getMethod().equals("ancestors")) {
            final Collection<Value> ancestors = new ArrayList<Value>();
            Node ancestor = modified;
            while (ancestor != null) {
                ancestors.add(getValueFactory().createValue(ancestor.getIdentifier()));
                try {
                    ancestor = ancestor.getParent();
                } catch (ItemNotFoundException ex) {
                    ancestor = null; // valid exception outcome, no parent because we are at root
                }
            }
            return ancestors.toArray(new Value[ancestors.size()]);
        } else {
            DerivedDataEngine.log.warn("Derived data definition contains unrecognized builtin reference, skipped");
            return null;
        }
    }

    @Override
    boolean persistPropertyValues(final Node modified, final Map<String, Value[]> parameters) {
        return false;
    }

    private String getMethod() throws RepositoryException {
        return node.getProperty("hipposys:method").getString();
    }

}
