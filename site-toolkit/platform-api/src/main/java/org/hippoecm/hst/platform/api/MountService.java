/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.api;

import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.onehippo.cms7.services.hst.Channel;

public interface MountService {

    /**
     * Returns the Map of all {@link Mount} objects of all {@link org.hippoecm.hst.platform.model.HstModel}s where the
     * key is the {@link Mount#getIdentifier()}. Note that the actual mount instances are returned, so for example
     * calling {@link Mount#getChannel()} and modify the {@link Channel} object will directly change the HST in memory
     * model and should in general never be done
     *
     * @param hostGroup the hostGroup to get the mounts for for all hst configurations
     * @return the Map of all live {@link Mount} objects of all {@link org.hippoecm.hst.platform.model.HstModel}s where the
     * key is the {@link Mount#getIdentifier()}
     */

    Map<String, Mount> getLiveMounts(String hostGroup);

    /**
     * Same as {@link #getLiveMounts(String)} only the mounts get decorated to preview mounts
     * @param hostGroup the hostGroup to get the mounts for for all hst configurations
     * @return The preview {@link Mount} objects
     * @see #getLiveMounts(String)
     */
    Map<String, Mount> getPreviewMounts(String hostGroup);
}
