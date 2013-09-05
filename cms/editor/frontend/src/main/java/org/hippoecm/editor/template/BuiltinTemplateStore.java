/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinTemplateStore implements ITemplateStore {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BuiltinTemplateStore.class);

    private ITypeLocator typeLocator;
    private ITemplateLocator locator;

    public BuiltinTemplateStore(ITypeLocator typeStore) {
        this.typeLocator = typeStore;
        locator = new TemplateLocator(new IStore[0]);
    }

    public ITemplateLocator getTypeLocator() {
        return this.locator;
    }

    /**
     * Set the type locator that will be used by type descriptors to resolve super
     * types.
     * @param locator
     */
    public void setTemplateLocator(ITemplateLocator locator) {
        this.locator = locator;
    }

    @Override
    public Iterator<IClusterConfig> find(Map<String, Object> criteria) {
        if (criteria.containsKey("type")) {
            List<IClusterConfig> list = new ArrayList<IClusterConfig>(1);
            list.add(new BuiltinTemplateConfig((ITypeDescriptor) criteria.get("type"), typeLocator, locator));
            return list.iterator();
        }
        return new ArrayList<IClusterConfig>(0).iterator();
    }

    @Override
    public IClusterConfig load(String id) throws StoreException {
        try {
            return new BuiltinTemplateConfig(typeLocator.locate(id), typeLocator, locator);
        } catch (StoreException ex) {
            throw new StoreException("No type found for " + id, ex);
        }
    }

    @Override
    public String save(IClusterConfig cluster) throws StoreException {
        throw new UnsupportedOperationException("Builtin template store is read only");
    }

    @Override
    public String save(final IClusterConfig object, final ITypeDescriptor type) throws StoreException {
        throw new UnsupportedOperationException("Builtin template store is read only");
    }

    @Override
    public void delete(IClusterConfig object) {
        throw new UnsupportedOperationException("Builtin template store is read only");
    }

    @Override
    public List<String> getMetadataEditors() {
        return Collections.emptyList();
    }

}
