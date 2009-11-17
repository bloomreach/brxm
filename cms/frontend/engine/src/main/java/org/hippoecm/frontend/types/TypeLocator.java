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
}
