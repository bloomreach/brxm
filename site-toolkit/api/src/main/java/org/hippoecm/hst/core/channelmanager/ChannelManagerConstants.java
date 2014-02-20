/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

public interface ChannelManagerConstants {

    static final String HST_MOUNT_ID = "HST-Mount-Id";
    static final String HST_SITE_ID = "HST-Site-Id";
    static final String HST_PAGE_ID = "HST-Page-Id";
    static final String HST_RENDER_VARIANT = "HST-Render-Variant";
    static final String HST_SITE_HAS_PREVIEW_CONFIG = "HST-Site-HasPreviewConfig";
    static final String HST_SITE_CHANGED_BY_SET = "HST-Changed-By-Set";
    // below used outside HST
    static final String HST_PAGE_REQUEST_VARIANTS = "HST-Page-Request-Variants";

    static final String HST_LOCKED_BY = "HST-LockedBy";
    static final String HST_LOCKED_BY_CURRENT_USER = "HST-LockedBy-Current-User";
    static final String HST_LOCKED_ON = "HST-LockedOn";
    static final String HST_LAST_MODIFIED = "HST-LastModified";

}
