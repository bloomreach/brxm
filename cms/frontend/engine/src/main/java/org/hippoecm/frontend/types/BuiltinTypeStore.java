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
package org.hippoecm.frontend.types;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinTypeStore implements ITypeStore {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinTypeStore.class);

    public ITypeDescriptor getTypeDescriptor(String type) {
        BuiltinTypeDescriptor result = new BuiltinTypeDescriptor(type);
        if (result.isValid()) {
            return result;
        }
        return null;
    }

    public List<ITypeDescriptor> getTypes(String namespace) {
        return null;
    }

    public void close() {
    }

    public void delete(ITypeDescriptor object) {
        throw new UnsupportedOperationException();
    }

    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) {
        if (criteria.containsKey("namespace")) {
            return getTypes((String) criteria.get("namespace")).iterator();
        }
        return null;
    }

    public ITypeDescriptor load(String id) {
        return getTypeDescriptor(id);
    }

    public String save(ITypeDescriptor object) {
        throw new UnsupportedOperationException();
    }

}
