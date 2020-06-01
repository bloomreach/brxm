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
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

class PropertyMapper {

    private final FunctionDescription function;
    private final Node modified;

    PropertyMapper(final FunctionDescription function, final Node modified) {
        this.function = function;
        this.modified = modified;
    }

    /* Creates a parameters map to be fed to the compute function.
    */
    Map<String, Value[]> getAccessedPropertyValues(final Collection<String> dependencies) throws RepositoryException {
        final Map<String, Value[]> parameters = new TreeMap<String, Value[]>();
        for (PropertyReference reference : function.getAccessedProperties()) {
            final Value[] values = reference.getPropertyValues(modified, dependencies);
            if (values != null) {
                parameters.put(reference.getName(), values);
            }
        }
        return parameters;
    }

    /* Use the definition of the derived properties to set the
    * properties computed by the function.
    */
    boolean persistDerivedPropertyValues(final Map<String, Value[]> parameters) throws RepositoryException {
        boolean changed = false;
        for (PropertyReference reference : function.getDerivedProperties()) {
            changed |= reference.persistPropertyValues(modified, parameters);
        }
        return changed;
    }

}
