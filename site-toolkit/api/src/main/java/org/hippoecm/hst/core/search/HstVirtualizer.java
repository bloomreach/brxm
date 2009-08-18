/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.search;

import javax.jcr.Node;

/**
 * A virtualizer implementation converts a physical node to its virtual context aware node, or
 * if the context wasn't virtual, it just returns the physical node
 *
 */
public interface HstVirtualizer {

    /**
     * @param canonical
     * @return the canonical node as a virtual node in the right context
     * @throws HstContextualizeException
     */
    Node virtualize(Node canonical) throws HstContextualizeException;
}
