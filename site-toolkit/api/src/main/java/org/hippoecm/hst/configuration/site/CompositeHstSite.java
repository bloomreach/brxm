/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.Map;
import org.onehippo.cms7.services.hst.Channel;

public interface CompositeHstSite extends HstSite {

    /**
     * @return the master
     */
    HstSite getMaster();

    /**
     * @return the immutable Map of all the branches for this {@link CompositeHstSite} and empty set if missing. The keys are
     * the {@link Channel#getBranchId()}
     */
    Map<String, HstSite> getBranches();
}
