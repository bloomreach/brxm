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
 * contribute to this {@link PageCacheKey} through {@link #setAttribute}.
 */
public interface PageCacheKey extends Serializable {

    /**
     * <p>
     *  All the combined attributes will make up the final {@link PageCacheKey}. Note that the <b>ORDER</b> in which the
     *  attributes are set <b>DO</b> influence the final created cachekey. Make sure that your added keyFragment object
     *  have a decent{@link #hashCode()} and {@link #equals(Object)} implementation that is also efficient as it will
     *  be used when storing objects in a cache with key {@link PageCacheKey}.
     * </p>
     * <p>
     *     The <code>subKey</code> can best be prefixed by namespacing (FQN of class calling the setAttribute) to
     *     avoid collisions. For example
     *     <pre>
     *     <code>
     *          PageCacheKey#setAttribute(MyValve.class.getName() + ".ip", ip-address);
     *     </code>
     *     </pre>
     * </p>
     * @param subKey the key to which the fragment belongs, not allowed to be <code>null</code>
     * @param keyFragment the fragment for the subKey, not allowed to be <code>null</code>
     */
    void setAttribute(String subKey, Serializable keyFragment);

}
