/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.channelmanager;

import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * Provides functions for contributing key-value pairs to attribute maps
 */
public interface AttributeContributor {

    /**
     * Uses the given window and request to compute the attributes that should be contributed to the given attribute
     * map. The returned map is a copy of the attributeMap but with possibly more entries or modified values, It is
     * assumed that upon execution of this method the thread-local variable {@link org.hippoecm.hst.core.request.HstRequestContext}
     * is not <code>null</code>.
     *
     * @param window       a hst component window for which to contribute attributes
     * @param request      the current request
     * @param attributeMap a map containing attributes as key-value pairs
     * @return a new attribute map with attribute contributions.
     */
    Map<String, String> contribute(HstComponentWindow window, HstRequest request, Map<String, String> attributeMap);

}
