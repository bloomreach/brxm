/*
 *  Copyright 2009 Hippo.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;

/**
 * The TypeLocator finds the ITypeDescriptor for a particular type,
 * given a number of {@link IStore}s.  It is intended to be used by
 * ITypeDescriptor implementations to be able to do their own lookup.
 * It should not be used by store implementations.
 */
public class TypeLocator implements ITypeLocator, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IStore<ITypeDescriptor>[] stores;
    
    public TypeLocator(IStore<ITypeDescriptor> stores[]) {
        this.stores = stores;
    }

    public ITypeDescriptor locate(String type) throws StoreException {
        for (int i = 0; i < stores.length; i++) {
            try {
                return stores[i].load(type);
            } catch (StoreException ex) {
                // ignore, continue
            }
        }
        throw new StoreException("type " + type + " was not found");
    }

    public List<ITypeDescriptor> getSubTypes(String type) throws StoreException {
        Map<String, ITypeDescriptor> types = new LinkedHashMap<String, ITypeDescriptor>();
        List<String> newList = new LinkedList<String>();
        newList.add(type);
        do {
            List<String> supertypes = new ArrayList<String>(newList);
            newList.clear();
            Map<String, Object> criteria = new HashMap<String, Object>();
            criteria.put("supertype", supertypes);
            for (int i = stores.length - 1; i >= 0; i--) {
                Iterator<ITypeDescriptor> storeResults = stores[i].find(criteria);
                while (storeResults.hasNext()) {
                    ITypeDescriptor descriptor = storeResults.next();
                    if (!types.containsKey(descriptor.getName())) {
                        newList.add(descriptor.getName());
                    }
                    types.put(descriptor.getName(), descriptor);
                }
            }
        } while (newList.size() > 0);
        return new LinkedList<ITypeDescriptor>(types.values());
    }

}
