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
package org.hippoecm.hst.core.internal;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.ResolvedMount;


/**
 * This is an INTERNAL USAGE ONLY API. Clients should not cast to these interfaces as they should never be used from client code
 * 
 */

public interface MutableResolvedMount extends ResolvedMount {

    /**
     * Sets the backing {@link Mount} behind the {@link ResolvedMount} to the new <code>mount</code>. This is useful 
     * to for example decorate a {@link Mount}: For example, to  decorate a {@link Mount} representing a live environment
     * in such a way that it shows the preview. 
     * @param mount
     */
    void setMount(Mount mount);
    
}
