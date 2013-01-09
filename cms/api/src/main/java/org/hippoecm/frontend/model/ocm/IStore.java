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
package org.hippoecm.frontend.model.ocm;

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.IClusterable;

/**
 * Store and retrieve objects.  It uses a lifecycle for objects that 
 * requires them to be saved explicitly.  The ids that are used must
 * not be interpreted. 
 * 
 * @param <T>
 */
public interface IStore<T> extends IClusterable {

    /**
     * Search for objects that meet the specified criteria.
     * 
     * @param criteria
     * @return iterator over objects that meet the criteria
     */
    Iterator<T> find(Map<String, Object> criteria) throws StoreException;

    /**
     * Load an object by its id.  A StoreException is thrown if the object cannot be found.
     * 
     * @param id
     * @return object with the specified id
     * @throws StoreException
     */
    T load(String id) throws StoreException;

    /**
     * Store an object.  The object need not be created by the store, it only needs to conform
     * to the interface T.  An id is generated for the object when one did not exist yet, otherwise
     * the existing id is returned.
     * 
     * @param object
     * @return id of the object
     * @throws StoreException
     */
    String save(T object) throws StoreException;

    /**
     * Remove an object from storage.
     * 
     * @param object
     * @throws StoreException
     */
    void delete(T object) throws StoreException;

}
