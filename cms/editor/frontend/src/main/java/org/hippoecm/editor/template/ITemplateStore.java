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
package org.hippoecm.editor.template;

import java.util.List;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.types.ITypeDescriptor;

public interface ITemplateStore extends IStore<IClusterConfig> {

    List<String> getMetadataEditors();

    /**
     * Store an cluster config for a document or compound type.
     *
     * @param object the cluster config to persist
     * @param type the type that can be edited by the cluster
     * @return id of the object
     * @throws org.hippoecm.frontend.model.ocm.StoreException
     */
    String save(IClusterConfig object, ITypeDescriptor type) throws StoreException;

}
