/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.type;

import java.util.List;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.DetachMonitor;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeLocator;

public class JcrTypeLocator implements ITypeLocator, IDetachable {
    private static final long serialVersionUID = 1L;


    private TypeLocator locator;
    private JcrTypeStore jcrTypeStore;

    public JcrTypeLocator() {
        jcrTypeStore = new JcrTypeStore();
        BuiltinTypeStore builtinTypeStore = new BuiltinTypeStore();
        locator = new TypeLocator(new IStore[] { jcrTypeStore, builtinTypeStore });
        jcrTypeStore.setTypeLocator(locator);
        builtinTypeStore.setTypeLocator(locator);
    }

    public List<ITypeDescriptor> getSubTypes(String type) throws StoreException {
        return locator.getSubTypes(type);
    }

    public ITypeDescriptor locate(String type) throws StoreException {
        return locator.locate(type);
    }

    public void detach() {
        locator.detach();
    }

}
