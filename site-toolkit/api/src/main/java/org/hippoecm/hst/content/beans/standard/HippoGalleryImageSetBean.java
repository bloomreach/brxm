/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.beans.standard;

/**
 * The interface the default hippo gallery set impls must implement.
 */
public interface HippoGalleryImageSetBean extends HippoBean {

    /**
     * @return the filename of the {@link HippoGalleryImageSetBean} and <code>null</code> when not present
     */
    String getFileName();
    
    /**
     * @return the description of the {@link HippoGalleryImageSetBean}  and <code>null</code> when not present
     */
    String getDescription();
    
    /**
     * @return the thumbnail image of this {@link HippoGalleryImageSetBean} or <code>null</code> when it cannot be retrieved
     */
    HippoGalleryImageBean getThumbnail();

    /**
     * @return the original image of this {@link HippoGalleryImageSetBean} or <code>null</code> when it cannot be retrieved
     */
    HippoGalleryImageBean getOriginal();
}
