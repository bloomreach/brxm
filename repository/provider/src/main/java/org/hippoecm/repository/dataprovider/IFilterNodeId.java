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
package org.hippoecm.repository.dataprovider;

import java.util.LinkedHashMap;

import org.apache.jackrabbit.spi.Name;

/**
 * All Virtual node id's that want to share their filter to other Virtual provider must implement this 
 * interface. Even if the virtual provider does not do anything itself with filtering, it might still want
 * to propagate filtering state through its virtual node id. 
 * 
 */
public interface IFilterNodeId {

    /**
     * whether this virtual node id is singledView, in other words, below its handle, show at most 1 single node
     * @return <code>true</code> when the virtual node id is singledView
     */
    boolean isSingledView();

    /**
     * Implementation can best return a new Map containing the key value pairs because the maps should not be changed
     * on an existing virtual node when being inherited by another provider
     * @return the LinkedHashMap<Name,String> containing the view filter. If no view, return <code>null</code> or empty map
     */
    LinkedHashMap<Name,String> getView();

    /**
     * Implementation can best return a new Map containing the key value pairs because the maps should not be changed
     * on an existing virtual node when being inherited by another provider
     * @return the LinkedHashMap<Name,String> containing the order filter. If no order, return <code>null</code> or empty map
     */
    LinkedHashMap<Name,String> getOrder();
}
