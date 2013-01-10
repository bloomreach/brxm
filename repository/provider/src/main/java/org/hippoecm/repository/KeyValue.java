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
package org.hippoecm.repository;

/**
 * Defines a simple key value pair.
 * A Map Entry has considerable additional semantics over and above a simple key-value pair.
 * This interface defines the minimum key value, with just the two get methods.
 */
public interface KeyValue<K,V>
{
    
    /**
     * Returns the key corresponding to the pair.
     *
     * @return the key corresponding to the pair
     */
    K getKey();

    /**
     * Returns the value corresponding to the pair.
     *
     * @return the value corresponding to the pair
     */
    V getValue();
    
}
