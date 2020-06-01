/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import javax.jcr.NodeIterator;

/**
 * Extends a #javax.jcr.NodeIterator interface, with an additional method.  Not all NodeIterator objects returned by
 * a repository are HippoNodeIterator objects, and when an object is a HippoNodeIterator the additional information
 * provided by these methods may still not be available.
 */
public interface HippoNodeIterator extends NodeIterator {

    /**
     * Obtains the number of elements that would be retrievable if the user your have access to all elements in the repository
     * @return the total size or -1 when not available.
     * @since 2.17.00
     */
     public long getTotalSize();
}
