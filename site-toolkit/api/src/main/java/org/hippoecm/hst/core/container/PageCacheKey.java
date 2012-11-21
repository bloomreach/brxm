/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.core.container;

import java.io.Serializable;

/**
 * A {@link PageCacheKey} represents a key that can be used to qualify/categorize some request. Different {@link Valve}s can
 * contribute to this {@link PageCacheKey} through {@link #append}.
 */
public interface PageCacheKey extends Serializable {

    /**
     * All the combined <code>keyFragment</code> will make up the key. Make sure that your added keyFragment object
     * have a decent {@link #hashCode()} and {@link #equals(Object)} implementation that is also efficient as it will
     * be used when storing objects in a cache with key {@link PageCacheKey}
     * @param keyFragment the fragment that will be added to the key. 
     */
    void append(Serializable keyFragment);

}
