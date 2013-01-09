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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.ITypeDescriptor;

public class JcrDraftStore implements IStore<ITypeDescriptor> {

    private static final long serialVersionUID = 1L;

    private final String prefix;
    private JcrTypeStore jcrTypeStore;

    public JcrDraftStore(JcrTypeStore typeStore, String prefix) {
        this.jcrTypeStore = typeStore;
        this.prefix = prefix;
    }

    public void delete(ITypeDescriptor object) throws StoreException {
        throw new StoreException();
    }

    public Iterator find(Map criteria) throws StoreException {
        return Collections.EMPTY_LIST.iterator();
    }

    public ITypeDescriptor load(String id) throws StoreException {
        ITypeDescriptor draft = null;
        if ((!"system".equals(prefix) && id.indexOf(':') > 0 && id.substring(0, id.indexOf(':')).equals(prefix))
                || ("system".equals(prefix) && id.indexOf(':') < 0)) {
            draft = jcrTypeStore.getDraftType(id);
        }
        if (draft == null) {
            throw new StoreException("Could not find draft");
        }
        return draft;
    }

    public String save(ITypeDescriptor object) throws StoreException {
        return jcrTypeStore.save(object);
    }
}
