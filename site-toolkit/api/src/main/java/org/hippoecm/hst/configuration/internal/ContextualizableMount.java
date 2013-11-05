/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.internal;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.site.HstSite;


/**
 * internal only api for being able to decorate a {@link Mount} to a preview {@link Mount}
 */
public interface ContextualizableMount extends MutableMount {

    /**
     * internal only : not api 
     * @return the preview hstSite of this mount. If this mount is already a preview mount, the same is returned as {@link #getHstSite()}.
     * Returned value can be <code>null</code> if this mount does not point to a hst:site as mountpoint
     */
    HstSite getPreviewHstSite();
    
}
