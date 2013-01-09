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
     * @return the preview content path of this mount. If there cannot be created a preview or this mount is already a preview, this returns the same as {@link #getContentPath()}
     */
    
    String getPreviewContentPath();
    
    /**
     * internal only : not api 
     * @return the preview canonical content path of this mount. If there cannot be created a preview or this mount is already a preview, this returns the same as {@link #getCanonicalContentPath()}
     */
    String getPreviewCanonicalContentPath();
    
    /**
     * internal only : not api 
     * @return the preview mount point of this mount. If this mount is already a preview mount, the same is returned as {@link #getMountPoint()}
     */
    String getPreviewMountPoint();

    /**
     * internal only : not api 
     * @return the preview hstSite of this mount. If this mount is already a preview mount, the same is returned as {@link #getHstSite()}
     */
    HstSite getPreviewHstSite();
    
}
