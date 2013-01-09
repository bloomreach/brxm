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

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Factory component to create HstRequestContext object.
 * 
 * This is an INTERNAL USAGE ONLY API. Clients should not cast to these interfaces as they should never be used from client code
 * 
 * @version $Id$
 */
public interface HstRequestContextComponent
{
    /**
     * Creates a mutable request context for either a servlet or portlet context.
     * <b>Note: this does not yet initializes a newly created HstPortletRequestContext.</b>
     * 
     * @param portletContext
     * @return
     */
    HstMutableRequestContext create(boolean portletContext);

    /**
     * Release a request context back to the context pool.
     * 
     * @param context
     */
    void release(HstRequestContext context);
}
